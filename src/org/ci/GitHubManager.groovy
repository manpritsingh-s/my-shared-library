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

    def getOpenPullRequests() {
        def token = getGitHubToken()
        def response = script.bash(
            script: """curl -s -H "Authorization: token ${token}" \\
                        https://api.github.com/repos/${repo}/pulls""",
            returnStdout: true
        )
        return script.readJSON(text: response)
    }

    def commentOnPR(prNumber, message) {
        def token = getGitHubToken()
        script.bash """
            curl -s -X POST -H "Authorization: token ${token}" \\
            -d '{ "body": "${message.replaceAll("\"", "\\\\\"")}" }' \\
            https://api.github.com/repos/${repo}/issues/${prNumber}/comments
        """
    }

    def closePullRequest(prNumber) {
        def token = getGitHubToken()
        script.bash"""
            curl -s -X PATCH -H "Authorization: token ${token}" \\
            -d '{ "state": "closed" }' \\
            https://api.github.com/repos/${repo}/pulls/${prNumber}
        """
    }

    def deleteBranch(branchName) {
        if (branchName == 'main' || branchName == 'master') {
            script.echo "Not deleting protected branch: ${branchName}"
            return
        }

        def token = getGitHubToken()
        script.bash """
            curl -s -X DELETE -H "Authorization: token ${token}" \\
            https://api.github.com/repos/${repo}/git/refs/heads/${branchName}
        """
    }
}
