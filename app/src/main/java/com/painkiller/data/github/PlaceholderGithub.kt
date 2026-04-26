package com.painkiller.data.github

// Gate 6 note:
// Contracts and orchestration for OAuth auth, repository/branch listing,
// and the single-file Git Data API commit flow now exist in this package
// (`GithubAuthRepository`, `GithubRepoBranchRepository`,
// `SingleFileCommitRepository`). Concrete HTTP client wiring of
// `GithubOAuthApi`, `GithubRepositoryApi`, and `GithubGitDataApi` is
// added in subsequent hardening gates.
internal object PlaceholderGithub
