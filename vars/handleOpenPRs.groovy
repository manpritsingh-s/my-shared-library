def getPullRequests(context, repo, tokenId) {
    def manager = new org.ci.GitHubManager(context, repo, tokenId)
    return manager.getPullRequests()
}

def getPullRequestsByLabel(context, repo, tokenId, label) {
    def manager = new org.ci.GitHubManager(context, repo, tokenId)
    return manager.getPullRequestsByLabel(label)
}

def filterPullRequestsByMinutes(context, prs, minutes) {
    def manager = new org.ci.GitHubManager(context, context.env.GITHUB_REPO, context.env.TOKEN_ID)
    def filtered = manager.filterPullRequests(prs, [minMinutes: minutes, unlabeledOnly: true])
    if (!filtered || filtered.isEmpty()) {
        context.echo "No pull requests found matching the filter (older than ${minutes} minutes and unlabeled)."
        return [message: "No PRs found", prs: []]
    }
    return [message: "PRs found", prs: filtered]
}

def filterOldLabeledPullRequests(context, prs, minutes, labelName) {
    def manager = new org.ci.GitHubManager(context, context.env.GITHUB_REPO, context.env.TOKEN_ID)
    def filtered = manager.filterPullRequests(prs, [minMinutes: minutes, label: labelName, labeledOnly: true])
    if (!filtered || filtered.isEmpty()) {
        context.echo "No pull requests found matching the filter (older than ${minutes} minutes and labeled '${labelName}')."
        return []
    }
    return filtered
}

def labelPullRequest(context, repo, tokenId, prNumber, labels) {
    def manager = new org.ci.GitHubManager(context, repo, tokenId)
    return manager.labelPullRequest(prNumber, labels)
}

def createLabel(context, repo, tokenId, labelName, color, description) {
    def manager = new org.ci.GitHubManager(context, repo, tokenId)
    return manager.createLabel(labelName, color, description)
}

def postWarningIfNeeded(context, repo, tokenId, prNumber, message, marker) {
    def manager = new org.ci.GitHubManager(context, repo, tokenId)
    return manager.postWarningIfNeeded(prNumber, message, marker)
}

def closePROnBuffer(context, repo, tokenId, prNumber, warningMarker, bufferMinutes) {
    def manager = new org.ci.GitHubManager(context, repo, tokenId)
    return manager.closePROnBuffer(prNumber, warningMarker, bufferMinutes)
}

def getPullRequestBranchName(context, repo, tokenId, prNumber) {
    def token = null
    context.withCredentials([context.string(credentialsId: tokenId, variable: 'GITHUB_TOKEN')]) {
        token = context.env.GITHUB_TOKEN
    }
    def prDetails = org.ci.GitHubHelpers.fetchPullRequestDetails(context, repo, token, prNumber)
    return prDetails?.head?.ref
}

def deleteBranch(context, repo, tokenId, branchName) {
    def manager = new org.ci.GitHubManager(context, repo, tokenId)
    return manager.deleteBranch(branchName)
}