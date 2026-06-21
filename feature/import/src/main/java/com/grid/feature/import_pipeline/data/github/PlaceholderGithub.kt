package com.grid.feature.import_pipeline.data.github

// Gate 7 note:
// Contracts and orchestration for OAuth auth, repository/branch listing,
// the single-file Git Data API commit flow, and the multi-file (folder /
// ZIP / .gitkeep) commit flow now exist in this package
// (`GithubAuthRepository`, `GithubRepoBranchRepository`,
// `SingleFileCommitRepository`, `MultiFileCommitRepository`). Concrete
// HTTP client wiring of `GithubOAuthApi`, `GithubRepositoryApi`, and
// `GithubGitDataApi` is added in subsequent hardening gates.
internal object PlaceholderGithub
