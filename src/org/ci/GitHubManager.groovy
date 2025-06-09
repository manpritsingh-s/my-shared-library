package org.ci

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
        def response = script.bat(script: """curl -s -H "Authorization: token ${token}" https://api.github.com/repos/${repo}/issues?state=open&per_page=100""",returnStdout: true).trim()

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
        def now = new Date()
        return prs.findAll { pr ->
            def createdAt = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", pr.created_at)
            def diff = (now.time - createdAt.time) / (1000 * 60 * 60 * 24)
            diff >= days
        }
    }

    def labelPullRequest(prNumber, labels) {
        def token = getGitHubToken()
        def payload = script.writeJSON(returnText: true, json: [labels: labels])
        def payload = payload.replaceAll
        script.bat(script: "curl -s -X POST -H \"Authorization: token ${token}\" -H \"Accept: application/vnd.github.v3+json\" -d \"${payload}\" https://api.github.com/repos/${repo}/issues/${prNumber}/labels")
    }

    def commentOnPR(prNumber, message) {
        def token = getGitHubToken()
        def escapedMessage = message
            .replaceAll('(["\\\\])', '\\\\$1')
            .replaceAll(/(\r\n|\n|\r)/, '\\\\n')
        def payload = "{ \"body":\"${escapedMessage}\"}"
        script.bat(
            script: "curl -s -X POST -H \"Authorization: token ${token}\" -H \"Accept: application/vnd.github.v3+json\" -d \"${payload}\" https://api.github.com/repos/${repo}/issues/${prNumber}/comments"
        )
    }

    def closePullRequest(prNumber) {
        def token = getGitHubToken()
        def payload = '{ "state": "closed" }'
        script.bat(
            script: "curl -s -X PATCH -H \"Authorization: token ${token}\" -H \"Accept: application/vnd.github.v3+json\" -d \"${payload}\" https://api.github.com/repos/${repo}/pulls/${prNumber}"
        )
    }

    def deleteBranch(branchName) {
        if (branchName == 'main' || branchName == 'master') {
            script.echo "Not deleting protected branch: ${branchName}"
            return
        }
        def token = getGitHubToken()
        def url = "https://api.github.com/repos/${repo}/git/refs/heads/${branchName}"
        script.bat(
            script: "curl -s -X DELETE -H \"Authorization: token ${token}\" ${url}"
        )
    }
}