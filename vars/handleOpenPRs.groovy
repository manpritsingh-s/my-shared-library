/**
* Fetch all pull requests for the given GitHub repository.
*
* @param script The Jenkins pipeline script context.
* @param repo The GitHub repository in the format owner/repo.
* @param tokenId Jenkins credentials ID for GitHub token.
* @return List of pull requests.
*/
def getPullRequests(script, repo, tokenId) {
    def manager = new org.ci.GitHubManager(script, repo, tokenId)
    return manager.getPullRequests()
}

/**
* Fetch all pull requests with the specified label.
*
* @param script The Jenkins pipeline script context.
* @param repo The GitHub repository in the format owner/repo.
* @param tokenId Jenkins credentials ID for GitHub token.
* @param label The label to filter pull requests by.
* @return List of pull requests with the specified label.
*/
def getPullRequestsByLabel(script, repo, tokenId, label) {
    def manager = new org.ci.GitHubManager(script, repo, tokenId)
    return manager.getPullRequestsByLabel(label)
}

/**
* Filter pull requests older than the specified minutes and with no labels.
*
* @param script The Jenkins pipeline script context.
* @param prs List of pull requests.
* @param minutes Minimum age of pull requests in minutes.
* @return Map with message and filtered list of pull requests.
*/
def filterPullRequestsByMinutes(script, prs, minutes) {
    def manager = new org.ci.GitHubManager(script, script.env.GITHUB_REPO, script.env.TOKEN_ID)
    def filtered = manager.filterPullRequests(prs, [minMinutes: minutes, unlabeledOnly: true])
    if (!filtered || filtered.isEmpty()) {
        script.echo "No pull requests found matching the filter (older than ${minutes} minutes and unlabeled)."
        return [message: "No PRs found", prs: []]
    }
    return [message: "PRs found", prs: filtered]
}

/**
* Filter pull requests older than the specified minutes and having the given label.
*
* @param script The Jenkins pipeline script context.
* @param prs List of pull requests.
* @param minutes Minimum age of pull requests in minutes.
* @param labelName The label to filter pull requests by.
* @return List of filtered pull requests.
*/
def filterOldLabeledPullRequests(script, prs, minutes, labelName) {
    def manager = new org.ci.GitHubManager(script, script.env.GITHUB_REPO, script.env.TOKEN_ID)
    def filtered = manager.filterPullRequests(prs, [minMinutes: minutes, label: labelName, labeledOnly: true])
    if (!filtered || filtered.isEmpty()) {
        script.echo "No pull requests found matching the filter (older than ${minutes} minutes and labeled '${labelName}')."
        return []
    }
    return filtered
}

/**
* Add the specified labels to the given pull request.
*
* @param script The Jenkins pipeline script context.
* @param repo The GitHub repository in the format owner/repo.
* @param tokenId Jenkins credentials ID for GitHub token.
* @param prNumber The pull request number.
* @param labels List of labels to add.
* @return GitHub API response.
*/
def labelPullRequest(script, repo, tokenId, prNumber, labels) {
    def manager = new org.ci.GitHubManager(script, repo, tokenId)
    return manager.labelPullRequest(prNumber, labels)
}

/**
* Create a new label in the repository if it does not exist.
*
* @param script The Jenkins pipeline script context.
* @param repo The GitHub repository in the format owner/repo.
* @param tokenId Jenkins credentials ID for GitHub token.
* @param labelName The name of the label to create.
* @param color The color of the label.
* @param description The description of the label.
* @return GitHub API response.
*/
def createLabel(script, repo, tokenId, labelName, color, description) {
    def manager = new org.ci.GitHubManager(script, repo, tokenId)
    return manager.createLabel(labelName, color, description)
}

/**
* Post a warning comment on the pull request if not already present.
*
* @param script The Jenkins pipeline script context.
* @param repo The GitHub repository in the format owner/repo.
* @param tokenId Jenkins credentials ID for GitHub token.
* @param prNumber The pull request number.
* @param message The warning message to post.
* @param marker Unique marker to identify the warning comment.
* @return Timestamp of the latest warning comment.
*/
def postWarningIfNeeded(script, repo, tokenId, prNumber, message, marker) {
    def manager = new org.ci.GitHubManager(script, repo, tokenId)
    return manager.postWarningIfNeeded(prNumber, message, marker)
}

/**
* Close the pull request if the buffer period after the warning has elapsed.
*
* @param script The Jenkins pipeline script context.
* @param repo The GitHub repository in the format owner/repo.
* @param tokenId Jenkins credentials ID for GitHub token.
* @param prNumber The pull request number.
* @param warningMarker Unique marker to identify the warning comment.
* @param bufferMinutes Buffer period in minutes.
* @return True if PR was closed, false otherwise.
*/
def closePROnBuffer(script, repo, tokenId, prNumber, warningMarker, bufferMinutes) {
    def manager = new org.ci.GitHubManager(script, repo, tokenId)
    return manager.closePROnBuffer(prNumber, warningMarker, bufferMinutes)
}

/**
* Retrieve the branch name for the given pull request.
*
* @param script The Jenkins pipeline script context.
* @param repo The GitHub repository in the format owner/repo.
* @param tokenId Jenkins credentials ID for GitHub token.
* @param prNumber The pull request number.
* @return The branch name.
*/
def getPullRequestBranchName(script, repo, tokenId, prNumber) {
    def token = null
    script.withCredentials([script.string(credentialsId: tokenId, variable: 'GITHUB_TOKEN')]) {
        token = script.env.GITHUB_TOKEN
    }
    def prDetails = org.ci.GitHubHelpers.fetchPullRequestDetails(script, repo, token, prNumber)
    return prDetails?.head?.ref
}

/**
* Delete the specified branch from the repository.
*
* @param script The Jenkins pipeline script context.
* @param repo The GitHub repository in the format owner/repo.
* @param tokenId Jenkins credentials ID for GitHub token.
* @param branchName The name of the branch to delete.
* @return GitHub API response.
*/
def deleteBranch(script, repo, tokenId, branchName) {
    def manager = new org.ci.GitHubManager(script, repo, tokenId)
    return manager.deleteBranch(branchName)
}
