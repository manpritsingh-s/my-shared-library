def getPullRequests(script, githubRepo, tokenId) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    return github.getPullRequests()
}

def filterPullRequests(script, prs, days) {
    def github = new org.ci.GitHubManager(script, script.env.GITHUB_REPO, script.env.TOKEN_ID)
    return github.filterPullRequests(prs, days)
}

def labelPullRequest(script, githubRepo, tokenId, prNumber, labels) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    github.labelPullRequest(prNumber, labels)
}

def commentOnPR(script, githubRepo, tokenId, prNumber, message) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    github.commentOnPR(prNumber, message)
}

def closePullRequest(script, githubRepo, tokenId, prNumber) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    github.closePullRequest(prNumber)
}

def deleteBranch(script, githubRepo, tokenId, branchName) {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    github.deleteBranch(branchName)
}

def createLabel(script, githubRepo, tokenId, labelName, color = "d73a4a", description = "") {
    def github = new org.ci.GitHubManager(script, githubRepo, tokenId)
    github.createLabel(labelName, color, description)
}