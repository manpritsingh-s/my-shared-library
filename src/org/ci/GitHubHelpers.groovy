package org.ci

class GitHubHelpers implements Serializable {

    static def fetchPullRequests(script, repo, token, label = null) {
        def url = "https://api.github.com/repos/${repo}/issues?state=open&per_page=100"
        if (label) {
            url += "&labels=${label}"
        }
        def response = script.bat(
            script: """curl -L -s -H "Authorization: Bearer ${token}" -H "Accept: application/vnd.github+json" -H "X-GitHub-Api-Version: 2022-11-28" "${url}" """,
            returnStdout: true
        ).trim()
        return extractPullRequestsFromApiResponse(script, response)
    }

    static def extractPullRequestsFromApiResponse(script, response) {
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

    static def fetchPullRequestDetails(script, repo, token, prNumber) {
        def url = "https://api.github.com/repos/${repo}/pulls/${prNumber}"
        def response = script.bat(
            script: """curl -L -s -H "Authorization: Bearer ${token}" -H "Accept: application/vnd.github+json" "${url}" """,
            returnStdout: true
        ).trim()
        return script.readJSON(text: response)
    }


    static def safeJsonForWindows(payload) {
        return payload.replace('"', '\\"')
    }
}