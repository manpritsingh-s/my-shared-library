package org.ci

import java.text.SimpleDateFormat

class GitHubManager implements Serializable {
    def script
    def repo
    def tokenId

    GitHubManager(script, String repo, String tokenId) {
        this.script = script
        this.repo = repo
        this.tokenId = tokenId
    }

    def getGitHubToken() {
        def token = null
        script.withCredentials([script.string(credentialsId: tokenId, variable: 'GITHUB_TOKEN')]) {
            token = script.env.GITHUB_TOKEN
        }
        return token
    }

    def getPullRequests() {
        def token = getGitHubToken()
        def response = script.bat(
            script: """curl -s -H "Authorization: token ${token}" "https://github.com/repos/${repo}/issues?state=open&per_page=100" """,
            returnStdout: true
        ).trim()

        script.echo "raw response content: ${response}"
        def jsonStart = response.indexOf('[')
        if (jsonStart > 0) {
            response = response.substring(jsonStart)
        }
        script.echo "Cleaned response content: ${response}"
        if (!response.startsWith("[")) {
            script.echo "ERROR: Response is not valid JSON"
            return []
        }
        try {
            def issues = script.readJSON(text: response)
            def prs = issues.findAll { it.pull_request }
            return prs
        } catch (Exception e) {
            script.echo "Failed to parse JSON: ${e.message}"
            return []
        }
    }

    def filterPullRequests(prs, days) {
        if (prs == null) {
            script.echo "No PRs provided to filter."
            return []
        }

        def now = new Date()
        script.echo "Filtering PRs older than ${days} days"

        def filtered = prs.findAll { pr ->
            try {
                if (!pr?.created_at) {
                    script.echo "Skipping item without created_at: ${pr}"
                    return false
                }

                script.echo "Checking PR #${pr.number} created at ${pr.created_at}"

                def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
                def dateToCheck = pr.updated_at ?: pr.created_at
                def updatedAt = sdf.parse(dateToCheck)
                def diff = (now.time - updatedAt.time) / (1000 * 60 * 60 * 24)

                script.echo "PR #${pr.number} is ${diff} days old"
                return diff >= days
            } catch (Exception e) {
                script.echo "Error parsing PR date for ${pr?.number ?: 'unknown'}: ${e.message}"
                return false
            }
        }
        return filtered ?: []
    }

    def labelPullRequest(prNumber, labels) {
        def token = getGitHubToken()
        def payload = script.writeJSON(returnText: true, json: [labels: labels])

        def curlCommand = """curl -L -X POST ^
            -H "Accept: application/vnd.github+json" ^
            -H "Authorization: Bearer ${token}" ^
            -H "X-GitHub-Api-Version: 2022-11-28" ^
            -H "Content-Type: application/json" ^
            https://github.com/repos/${repo}/issues/${prNumber}/labels ^
            -d '${payload}'"""

        script.echo "Making request to GitHub API..."
        script.echo "Payload: ${payload}"

        def response = script.bat(
            script: curlCommand,
            returnStdout: true
        ).trim()

        script.echo "GitHub API Response: ${response}"
        return response
    }

    def commentOnPR(prNumber, message) {
        def token = getGitHubToken()
        def escapedMessage = message
            .replaceAll('(["\\\\])', '\\\\$1')
            .replaceAll(/(\r\n|\n|\r)/, '\\\\n')
        def jsonPayload = script.writeJSON(returnText: true, json: [body: escapedMessage])
        
        def curlCommand = """curl -s -X POST ^
            -H "Authorization: token ${token}" ^
            -H "Accept: application/vnd.github.v3+json" ^
            -H "Content-Type: application/json" ^
            -d ${jsonPayload} ^
            https://api.github.com/repos/${repo}/issues/${prNumber}/comments"""
            
        script.echo "Making request to GitHub API..."
        def response = script.bat(script: curlCommand, returnStdout: true).trim()
        
        script.echo "GitHub API Response: ${response}"
        return response
    }

    def closePullRequest(prNumber) {
        def token = getGitHubToken()
        def payload = script.writeJSON(returnText: true, json: [state: 'closed'])
        
        def curlCommand = """curl -s -X PATCH ^
            -H "Authorization: token ${token}" ^
            -H "Accept: application/vnd.github.v3+json" ^
            -H "Content-Type: application/json" ^
            -d ${payload} ^
            https://api.github.com/repos/${repo}/pulls/${prNumber}"""
            
        script.bat(
            script: curlCommand,
            returnStdout: true
        )
    }

    def deleteBranch(branchName) {
        if (branchName == 'main' || branchName == 'master') {
            script.echo "Not deleting protected branch: ${branchName}"
            return
        }
        def token = getGitHubToken()
        def url = "https://api.github.com/repos/${repo}/git/refs/heads/${branchName}"
        
        def curlCommand = """curl -s -X DELETE ^
            -H "Authorization: token ${token}" ^
            -H "Accept: application/vnd.github.v3+json" ^
            ${url}"""
            
        script.bat(
            script: curlCommand,
            returnStdout: true
        )
    }
}