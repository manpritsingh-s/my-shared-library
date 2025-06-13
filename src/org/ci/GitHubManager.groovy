package org.ci

import org.ci.GitHubHelpers
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

    /**
     * Retrieve the GitHub token from Jenkins credentials.
     *
     * @return The GitHub token as a string.
     */
    def getGitHubToken() {
        def token = null
        script.withCredentials([script.string(credentialsId: tokenId, variable: 'GITHUB_TOKEN')]) {
            token = script.env.GITHUB_TOKEN
        }
        return token
    }

    /**
     * Fetch all open pull requests from the GitHub repository.
     *
     * @return List of pull requests.
     */
    def getPullRequests() {
        def token = getGitHubToken()
        return GitHubHelpers.fetchPullRequests(script, repo, token)
    }

    /**
     * Fetch pull requests by label from the GitHub repository.
     *
     * @param label, The label to filter pull requests by.
     * @return List of pull requests with the specified label.
     */
    def getPullRequestsByLabel(String label) {
        def token = getGitHubToken()
        return GitHubHelpers.fetchPullRequests(script, repo, token, label)
    }

    /**
     * Fetch all comments for a pull request.
     *
     * @param prNumber Pull Request number.
     * @return List of comments (each as a map), or empty list if none/error.
     */
    def getPRComments(prNumber) {
        def token = getGitHubToken()
        return GitHubHelpers.fetchPRComments(script, repo, token, prNumber)
    }

    /**
    * Filters pull requests older than the specified number of days.
    *
    * @param prs, List of pull requests.
    * @param days, Number of days to filter by.
    * @return List of filtered pull requests.
    */
// def filterPullRequests(prs, days) {
//     if (!prs) {
//         script.echo "No PRs provided to filter."
//         return []
//     }
//     def now = new Date()
//     script.echo "Filtering PRs older than ${days} days"
//     def filtered = prs.findAll { pr ->
//         try {
//             if (!pr || !pr.created_at) {
//                 script.echo "Skipping item without created_at: ${pr}"
//                 return false
//             }
//             script.echo "Checking PR #${pr.number} created at ${pr.created_at}"
//             def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
//             sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
//             def dateToCheck = pr.updated_at ?: pr.created_at
//             def updatedAt = sdf.parse(dateToCheck)
//             def diff = (now.time - updatedAt.time) / (1000 * 60 * 60 * 24)
//             script.echo "PR #${pr.number} is ${diff} days old"
//             return diff >= days
//         } catch (Exception e) {
//             script.echo "Error parsing PR date for ${pr?.number ?: 'unknown'}: ${e.message}"
//             return false
//         }
//     }
//     return filtered ?: []
// }


// In GitHubManager.groovy

/**
 * Filters pull requests older than the specified number of minutes.
 *
 * @param prs List of pull requests.
 * @param minutes Number of minutes to filter by.
 * @return List of filtered pull requests.
 */
def filterPullRequestsByMinutes(prs, minutes) {
    if (!prs) {
        script.echo "No PRs provided to filter."
        return []
    }
    def now = new Date()
    script.echo "Filtering PRs older than ${minutes} minutes"
    def filtered = prs.findAll { pr ->
        try {
            if (!pr || !pr.created_at) {
                script.echo "Skipping item without created_at: ${pr}"
                return false
            }
            def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
            def dateToCheck = pr.updated_at ?: pr.created_at
            def updatedAt = sdf.parse(dateToCheck)
            def diffMinutes = (now.time - updatedAt.time) / (1000 * 60)
            script.echo "PR #${pr.number} is ${diffMinutes} minutes old"
            return diffMinutes >= minutes
        } catch (Exception e) {
            script.echo "Error parsing PR date for ${pr?.number ?: 'unknown'}: ${e.message}"
            return false
        }
    }
    return filtered ?: []
}

    /*
    * Add labels to a pull request.
    *
    * @param prNumber, Pull Request number.
    * @param labels, List of labels to add to the pull request.
    * @return The response from the GitHub API.
    */
    def labelPullRequest(prNumber, labels) {
        def token = getGitHubToken()
        def payload = script.writeJSON(returnText: true, json: [labels: labels])
        def safePayload = GitHubHelpers.safeJsonForWindows(payload)

        def curlCommand = """curl -L ^
            -H "Accept: application/vnd.github+json" ^
            -H "Authorization: Bearer ${token}" ^
            -H "X-GitHub-Api-Version: 2022-11-28" ^
            https://api.github.com/repos/${repo}/issues/${prNumber}/labels ^
            -d "${safePayload}" """

        script.echo "Making request to GitHub API to add labels..."
        script.echo "Payload: ${payload}"

        def response = script.bat(
            script: curlCommand,
            returnStdout: true
        ).trim()

        script.echo "GitHub API Response: ${response}"
        return response
    }

    /**
     * Create a label in the GitHub repository if it does not exist.
     *
     * @param labelName, The name of the label to create.
     * @param color, The color of the label.
     * @param description, The description of the label.
     * @return The response from the GitHub API.
     */
    def createLabel(labelName, color = "red", description = "") {
        def token = getGitHubToken()
        def payload = script.writeJSON(returnText: true, json: [
            name: labelName,
            color: color,
            description: description
        ])
        def safePayload = GitHubHelpers.safeJsonForWindows(payload)

        def curlCommand = """curl -L ^
            -H "Accept: application/vnd.github+json" ^
            -H "Authorization: Bearer ${token}" ^
            -H "X-GitHub-Api-Version: 2022-11-28" ^
            https://api.github.com/repos/${repo}/labels ^
            -d "${safePayload}" """

        script.echo "Creating label '${labelName}' if it does not exist..."
        script.echo "Payload: ${payload}"

        def response = script.bat(
            script: curlCommand,
            returnStdout: true
        ).trim()

        script.echo "GitHub API Response: ${response}"
        return response
    }

    /**
     * Add a comment to a pull request.
     *
     * @param prNumber, Pull Request number.
     * @param message, The comment message to add to the pull request.
     * @return The response from the GitHub API.
     */
    def commentOnPR(prNumber, message) {
        def token = getGitHubToken()
        def jsonPayload = script.writeJSON(returnText: true, json: [body: message])
        def safePayload = GitHubHelpers.safeJsonForWindows(jsonPayload)

        def curlCommand = """curl -L ^
            -H "Accept: application/vnd.github+json" ^
            -H "Authorization: Bearer ${token}" ^
            -H "X-GitHub-Api-Version: 2022-11-28" ^
            https://api.github.com/repos/${repo}/issues/${prNumber}/comments ^
            -d "${safePayload}" """

        script.echo "Making request to GitHub API..."
        def response = script.bat(script: curlCommand, returnStdout: true).trim()

        script.echo "GitHub API Response: ${response}"
        return response
    }

    /**
     * Close a pull request.
     *
     * @param prNumber, Pull Request number.
     * @return The response from the GitHub API.
     */
    def closePullRequest(prNumber) {
        def token = getGitHubToken()
        def payload = script.writeJSON(returnText: true, json: [state: 'closed'])
        def safePayload = GitHubHelpers.safeJsonForWindows(payload)

        def curlCommand = """curl -L ^
            -X PATCH ^
            -H "Accept: application/vnd.github+json" ^
            -H "Authorization: Bearer ${token}" ^
            -H "X-GitHub-Api-Version: 2022-11-28" ^
            https://api.github.com/repos/${repo}/pulls/${prNumber} ^
            -d "${safePayload}" """

        script.bat(
            script: curlCommand,
            returnStdout: true
        )
    }

    /**
     * Delete a branch from the GitHub repository.
     *
     * @param branchName, The name of the branch to delete.
     * @return The response from the GitHub API.
     */
    def deleteBranch(branchName) {
        if (branchName == 'main' || branchName == 'master') {
            script.echo "Not deleting protected branch: ${branchName}"
            return
        }
        def token = getGitHubToken()
        def encodedBranch = java.net.URLEncoder.encode(branchName, "UTF-8")
        def url = "https://api.github.com/repos/${repo}/git/refs/heads/${encodedBranch}"
        def curlCommand = """curl -v -L -X DELETE -H "Accept: application/vnd.github+json" -H "Authorization: Bearer ${token}" "${url}" """
        script.echo "Deleting branch with command: ${curlCommand}"
        def response = script.bat(
            script: curlCommand,
            returnStdout: true
        ).trim()
        script.echo "Delete branch API response: ${response}"
        return response
    }

    /**
     * Posts a warning comment to a PR only if one does not already exist, and returns the timestamp of the latest warning comment.
     *
     * @param prNumber Pull Request number.
     * @param message The warning message to post (should include a unique marker).
     * @param marker Unique marker string to identify warning comments.
     * @return The timestamp of the latest warning comment (if any), or null.
     */
    def postWarningIfNeeded(prNumber, message, marker) {
        def token = getGitHubToken()
        def comments = GitHubHelpers.fetchPRComments(script, repo, token, prNumber)
        def latest = GitHubHelpers.findLatestWarningComment(comments, marker)
        if (latest) {
            script.echo "Warning comment already exists for PR #${prNumber} at ${latest.created_at}"
            return latest.created_at
        }
        commentOnPR(prNumber, message)
        script.echo "Posted new warning comment for PR #${prNumber}"
        // Fetch again to get the timestamp of the new comment
        comments = GitHubHelpers.fetchPRComments(script, repo, token, prNumber)
        latest = GitHubHelpers.findLatestWarningComment(comments, marker)
        return latest?.created_at
    }

    /**
     * Closes a PR only if the buffer period has elapsed since the last warning comment.
     *
     * @param prNumber Pull Request number.
     * @param marker Unique marker string to identify warning comments.
     * @param bufferHours Buffer period in hours to wait after the last warning comment.
     * @return true if PR was closed, false otherwise.
     */
    // def closePROnBuffer(prNumber, marker, bufferHours) {
    //     def token = getGitHubToken()
    //     def comments = GitHubHelpers.fetchPRComments(script, repo, token, prNumber)
    //     def latest = GitHubHelpers.findLatestWarningComment(comments, marker)
    //     if (!latest) {
    //         script.echo "No warning comment found for PR #${prNumber}. Not closing."
    //         return false
    //     }
    //     def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    //     sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
    //     def lastWarning = sdf.parse(latest.created_at)
    //     def now = new Date()
    //     def diffHours = (now.time - lastWarning.time) / (1000 * 60 * 60)
    //     if (diffHours < bufferHours) {
    //         script.echo "Buffer period not elapsed for PR #${prNumber}: ${diffHours}h < ${bufferHours}h"
    //         return false
    //     }
    //     closePullRequest(prNumber)
    //     script.echo "Closed PR #${prNumber} after buffer period."
    //     return true
    // }

    /**
     * Closes a PR only if the buffer period has elapsed since the last warning comment.
     *
     * @param prNumber Pull Request number.
     * @param warningMarker Unique marker string to identify warning comments.
     * @param bufferMinutes Buffer period in minutes to wait after the last warning comment.
     * @return true if PR was closed, false otherwise.
     */
    def closePROnBuffer(prNumber, warningMarker, bufferMinutes) {
        def lastWarningTime = getLatestWarningCommentTimestamp(prNumber, warningMarker)
        if (!lastWarningTime) {
            script.echo "No warning comment found, not closing PR."
            return false
        }
        def now = new Date()
        def diffMinutes = (now.time - lastWarningTime.time) / (1000 * 60)
        script.echo "Buffer diff: ${diffMinutes} minutes (buffer: ${bufferMinutes})"
        if (diffMinutes > bufferMinutes) {
            script.echo "Buffer elapsed, closing PR #${prNumber}"
            closePullRequest(prNumber)
            return true
        }
        script.echo "Buffer period not elapsed: ${diffMinutes} < ${bufferMinutes} minutes"
        return false
    }

    /**
     * Get the timestamp of the latest warning comment.
     *
     * @param prNumber Pull Request number.
     * @param warningMarker Unique marker string to identify warning comments.
     * @return Date of latest warning comment, or null.
     */
    def getLatestWarningCommentTimestamp(prNumber, warningMarker) {
        def comments = getPRComments(prNumber)
        def warningComments = comments.findAll { it.body?.contains(warningMarker) }
        if (!warningComments) return null
        def latest = warningComments.max { it.created_at }
        def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
        return sdf.parse(latest.created_at)
    }
}