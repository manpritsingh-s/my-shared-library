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

        def response = script.bat(script: """curl -s -H "Authorization: token ${token}" https://api.github.com/repos/${repo}/pulls""", returnStdout: true).trim()
        // def splitIndex = response.lastIndexOf('\n')
        // def body = response.substring(0, splitIndex)
        // def status = response.substring(splitIndex + 1)

        // script.echo "HTTP status: ${status}"
        // script.echo "Response body: ${body}"

        // if (status != "200") {
        //     script.echo "ERROR: GitHub API call failed."
        //     return []
        // }
        script.echo "------------Response from GitHub API is going to start -------------"

        script.echo "Response content: ${response}"
        if (!response.startsWith("[") && !response.startsWith("{")) {
        script.echo "ERROR: Response is not valid JSON. Aborting."
        return []
        }
        script.echo "------------Write File is going to start -------------"

        // script.writeFile file: 'github_response.json', text: response

        try {
            def prs = script.readJSON(text: response)
            script.echo "--------------Open Pull Requests:------------------"
            prs.each { pr ->
                script.echo "#${pr.number}: ${pr.title}"
                script.echo "------------try block got failed -------------"
            }
            return prs
        } catch (Exception e) {
            script.echo "------------catch block got started -------------"
            script.echo "Failed to parse JSON: ${e.message}"
            script.echo "------------Json Field cannot be read -------------"
        }
    }

    def commentOnPR(prNumber, message) {
        def token = getGitHubToken()
        def escapedMessage = message.replaceAll('"', '\\\\"')
        def payload = "{ \"body\": \"${escapedMessage}\" }"

        script.bat(
            script: "curl -s -X POST -H \"Authorization: token ${token}\" -d \"${payload}\" https://api.github.com/repos/${repo}/issues/${prNumber}/comments"
        )
    }

    def closePullRequest(prNumber) {
        def token = getGitHubToken()
        def payload = '{ "state": "closed" }'

        script.bat(
            script: "curl -s -X PATCH -H \"Authorization: token ${token}\" -d \"${payload}\" https://api.github.com/repos/${repo}/pulls/${prNumber}"
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
