def call(String githubRepo, String tokenId = 'github-token') {
    def github = new org.ci.GitHubManager(this, githubRepo, tokenId)

    def messageTemplate = libraryResource('pr_notification_template.txt')

    def prs = github.getOpenPullRequests()

    prs.each { pr ->
        def prNumber = pr.number
        def prAuthor = pr.user.login
        def prBranch = pr.head.ref

        echo "Processing PR #${prNumber} from ${prAuthor}"

        def personalizedMessage = messageTemplate.replace('${author}', prAuthor)

        github.commentOnPR(prNumber, personalizedMessage)

        echo "Waiting 10 minutes before closing PR #${pr.number}"
        sleep(time: 10, unit: 'MINUTES')

        github.closePullRequest(prNumber)

        github.deleteBranch(prBranch)

        github.commentOnPR(prNumber, "PR #${prNumber} has been closed and branch `${prBranch}` was deleted.")
    }
}
