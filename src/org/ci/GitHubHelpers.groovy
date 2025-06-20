package org.ci

class GitHubHelpers implements Serializable {

    /**
     * Fetches open pull requests from a GitHub repository.
     *
     * @param script, The Jenkins pipeline script context.
     * @param repo, The GitHub repository in the format owner/repo.
     * @param token, The GitHub API token.
     * @param label, Label to filter pull requests.
     * @return List of pull requests.
     */
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

    /**
     * Extracts pull requests from the API response.
     *
     * @param script, The Jenkins pipeline script context.
     * @param response, The raw JSON response string from GitHub API.
     * @return List of pull requests extracted from the response.
     */
    static def extractPullRequestsFromApiResponse(script, response) {
        // script.echo "raw response content: ${response}"
        def jsonStart = response.indexOf('[')
        if (jsonStart > 0) {
            response = response.substring(jsonStart)
        }
        // script.echo "Cleaned response content: ${response}"
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

    /**
    * Fetches details for a specific pull request from the GitHub API.
    *
    * @param script Jenkins pipeline script context.
    * @param repo GitHub repository in owner/repo format.
    * @param token GitHub API token.
    * @param prNumber Pull request number.
    * @return Pull request details as a map, or null if not found/invalid.
    */
    static def fetchPullRequestDetails(script, repo, token, prNumber) {
        def url = "https://api.github.com/repos/${repo}/pulls/${prNumber}"
        def response = script.bat(
            script: """curl -L -s -H "Authorization: Bearer ${token}" -H "Accept: application/vnd.github+json" "${url}" """,
            returnStdout: true
        ).trim()

        def jsonStart = response.indexOf('{')
        if (jsonStart > 0) {
            response = response.substring(jsonStart)
        }
        // script.echo "Cleaned PR details response: ${response}"
        if (!response?.startsWith("{")) {
            script.echo "ERROR: Response is not valid JSON: ${response}"
            return null
        }
        try {
            def prDetails = script.readJSON(text: response)
            script.echo "PR head: ${prDetails?.head}"
            script.echo "PR head.ref: ${prDetails?.head?.ref}"
            return prDetails
        } catch (Exception e) {
            script.echo "Failed to parse PR details JSON: ${e.message}"
            return null
        }
    }

    /**
     * Fetches all comments for a pull request.
     *
     * @param script Jenkins pipeline script context.
     * @param repo GitHub repository in owner/repo format.
     * @param token GitHub API token.
     * @param prNumber Pull request number.
     * @return List of comments (each as a map), or empty list if none/error.
     */
    static def fetchPRComments(script, repo, token, prNumber) {
        def url = "https://api.github.com/repos/${repo}/issues/${prNumber}/comments?per_page=100"
        def response = script.bat(
            script: """curl -L -s -H \"Authorization: Bearer ${token}\" -H \"Accept: application/vnd.github+json\" \"${url}\" """,
            returnStdout: true
        ).trim()
        def jsonStart = response.indexOf('[')
        if (jsonStart > 0) {
            response = response.substring(jsonStart)
        }
        if (!response?.startsWith("[")) {
            script.echo "ERROR: PR comments response is not valid JSON: ${response}"
            return []
        }
        try {
            return script.readJSON(text: response)
        } catch (Exception e) {
            script.echo "Failed to parse PR comments JSON: ${e.message}"
            return []
        }
    }

    /**
     * Finds the latest warning comment (with a unique marker) and its timestamp.
     *
     * @param comments List of PR comments (as returned by fetchPRComments).
     * @param marker Unique marker string to identify warning comments.
     * @return Map with keys 'comment' (the comment map) and 'created_at' (timestamp), or null if not found.
     */
    static def findLatestWarningComment(comments, marker) {
        if (!comments) return null
        def warningComments = comments.findAll { it.body?.contains(marker) }
        if (!warningComments) return null
        def latest = warningComments.max { it.created_at }
        return [comment: latest, created_at: latest.created_at]
    }

    /**
    * Escapes double quotes in a JSON payload for Windows command line compatibility.
    *
    * @param payload JSON string to escape.
    * @return Escaped JSON string.
     */
    static def safeJsonForWindows(payload) {
        return payload.replace('"', '\\"')
    }
}