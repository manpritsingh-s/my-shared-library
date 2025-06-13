/**
* Fetch all request for the given GitHub repository.
*
* @param script, The Jenkins pipeline script context.
* @param githubRepo, The GitHub repository in the format owner/repo.
* @param tokenId, Jenkins credentials ID for GitHub token.
* @return List of pull requests.
*/
def getPullRequests(script, githubRepo, tokenId) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    return github.getPullRequests()
}

/**
* Filter pull requests that are older than the specified number of days.
*
* @param script, The Jenkins pipeline script context.
* @param prs, List of pull requests to filter.
* @param days, The number of days to filter by.
* @return List of filtered pull requests.
*/
// def filterPullRequests(script, prs, days) {
//     def github = new org.ci.GitHubManager(script, script.env.GITHUB_REPO, script.env.TOKEN_ID)
//     return github.filterPullRequests(prs, days)
// }

// def filterPullRequests(script, prs, days) {
//     def github = new org.ci.GitHubManager(script, script.env.GITHUB_REPO, script.env.TOKEN_ID)
//     def filtered = github.filterPullRequests(prs, days)
//     if (!filtered || filtered.isEmpty()) {
//         script.echo "No pull requests found matching the filter (older than ${days} days)."
//         return [message: "No PRs found", prs: []]
//     }
//     return [message: "PRs found", prs: filtered]
// }


def filterPullRequestsByMinutes(script, prs, minutes) {
    def github = new org.ci.GitHubManager(script, script.env.GITHUB_REPO, script.env.TOKEN_ID)
    def filtered = github.filterPullRequestsByMinutes(prs, minutes)
    if (!filtered || filtered.isEmpty()) {
        script.echo "No pull requests found matching the filter (older than ${minutes} minutes)."
        return [message: "No PRs found", prs: []]
    }
    return [message: "PRs found", prs: filtered]
}

/*
* Add labels to a pull request.
*
* @param script, The Jenkins pipeline script context.
* @param githubRepo, The GitHub repository in the format owner/repo.
* @param tokenId, Jenkins credentials ID for GitHub token.
* @param prNumber, Pull Request number.
* @param labels, List of labels to add to the pull request.
* @return void
*/
def labelPullRequest(script, githubRepo, tokenId, prNumber, labels) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    github.labelPullRequest(prNumber, labels)
}

/*
* Add a comment to a pull request.
*
* @param script, The Jenkins pipeline script context.
* @param githubRepo, The GitHub repository in the format owner/repo.
* @param tokenId, Jenkins credentials ID for GitHub token.
* @param prNumber, Pull Request number.
* @param message, The comment message to add to the pull request.
* @return void
*/
def commentOnPR(script, githubRepo, tokenId, prNumber, message) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    github.commentOnPR(prNumber, message)
}

/*
* Close a pull request.
*
* @param script, The Jenkins pipeline script context.
* @param githubRepo, The GitHub repository in the format owner/repo.
* @param tokenId, Jenkins credentials ID for GitHub token.
* @param prNumber, Pull Request number.
* @return void
*/
def closePullRequest(script, githubRepo, tokenId, prNumber) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    github.closePullRequest(prNumber)
}

/*
* Delete a branch from the GitHub repository.
*
* @param script, The Jenkins pipeline script context.
* @param githubRepo, The GitHub repository in the format owner/repo.
* @param tokenId, Jenkins credentials ID for GitHub token.
* @param branchName, The name of the branch to delete.
* @return void
*/
def deleteBranch(script, githubRepo, tokenId, branchName) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    github.deleteBranch(branchName)
}

/*
* Create a label in the GitHub repository if it does not exist.
*
* @param script, The Jenkins pipeline script context.
* @param githubRepo, The GitHub repository in the format owner/repo.
* @param tokenId, Jenkins credentials ID for GitHub token.
* @param labelName, The name of the label to create.
* @param color, The color code of the label.
* @param description, The description for the label.
* @return void
*/
def createLabel(script, githubRepo, tokenId, labelName, color = "d73a4a", description = "") {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    github.createLabel(labelName, color, description)
}

/*
* Fetch pull requests by label.
*
* @param script, The Jenkins pipeline script context.
* @param githubRepo, The GitHub repository in the format owner/repo.
* @param tokenId, Jenkins credentials ID for GitHub token.
* @param label, The label to filter pull requests.
* @return List of pull requests with the Label.
*/
def getPullRequestsByLabel(script, githubRepo, tokenId, label) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    return github.getPullRequestsByLabel(label)
}

/*
* Get the branch name for a pull request.
*
* @param script, The Jenkins pipeline script context.
* @param githubRepo, The GitHub repository in the format owner/repo.
* @param tokenId, Jenkins credentials ID for GitHub token.
* @param prNumber, Pull Request number.
* @return branch name or null if not found.
*/
def getPullRequestBranchName(script, githubRepo, tokenId, prNumber) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    def token = github.getGitHubToken()
    def prDetails = org.ci.GitHubHelpers.fetchPullRequestDetails(script, githubRepo, token, prNumber)
    return prDetails?.head?.ref
}

/**
* Post a warning comment to a PR only if one does not already exist (prevents duplicates).
*
* @param script Jenkins pipeline script context.
* @param githubRepo GitHub repository in owner/repo format.
* @param tokenId Jenkins credentials ID for GitHub token.
* @param prNumber Pull Request number.
* @param message The warning message (should include a unique marker).
* @param marker Unique marker string to identify warning comments.
* @return The timestamp of the latest warning comment (if any), or null.
*/
def postWarningIfNeeded(script, githubRepo, tokenId, prNumber, message, marker) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    return github.postWarningIfNeeded(prNumber, message, marker)
}

/**
* Close a PR only if the buffer period has elapsed since the last warning comment.
*
* @param script Jenkins pipeline script context.
* @param githubRepo GitHub repository in owner/repo format.
* @param tokenId Jenkins credentials ID for GitHub token.
* @param prNumber Pull Request number.
* @param marker Unique marker string to identify warning comments.
* @param bufferMinutes Buffer period in minutes to wait after the last warning comment.
* @return true if PR was closed, false otherwise.
*/
def closePROnBuffer(script, githubRepo, tokenId, prNumber, warningMarker, bufferMinutes) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    return github.closePROnBuffer(prNumber, warningMarker, bufferMinutes)
}

/**
* Get all comments for a PR.
*
* @param script Jenkins pipeline script context.
* @param githubRepo GitHub repository in owner/repo format.
* @param tokenId Jenkins credentials ID for GitHub token.
* @param prNumber Pull Request number.
* @return List of comments.
*/
def getPRComments(script, githubRepo, tokenId, prNumber) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    return github.getPRComments(prNumber)
}

/**
* Get the timestamp of the latest warning comment.
*
* @param script Jenkins pipeline script context.
* @param githubRepo GitHub repository in owner/repo format.
* @param tokenId Jenkins credentials ID for GitHub token.
* @param prNumber Pull Request number.
* @param warningMarker Unique marker string to identify warning comments.
* @return Date of latest warning comment, or null.
*/
def getLatestWarningCommentTimestamp(script, githubRepo, tokenId, prNumber, warningMarker) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    return github.getLatestWarningCommentTimestamp(script, githubRepo, tokenId, prNumber, warningMarker)
}

/**
* Filter PRs by label.
*
* @param prs List of PRs.
* @param label Label to filter by.
* @return List of PRs that have the label.
*/
def filterPRsByLabel(prs, label) {
    if (!prs) return []
    return prs.findAll { pr ->
        pr.labels && pr.labels.any { it.name == label }
    }
}