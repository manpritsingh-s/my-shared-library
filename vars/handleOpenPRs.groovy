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
def filterPullRequests(script, prs, days) {
    def github = new org.ci.GitHubManager(script, script.env.GITHUB_REPO, script.env.TOKEN_ID)
    return github.filterPullRequests(prs, days)
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