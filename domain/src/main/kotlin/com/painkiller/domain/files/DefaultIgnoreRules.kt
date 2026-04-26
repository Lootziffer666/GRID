package com.painkiller.domain.files

object DefaultIgnoreRules {
    val rules: List<IgnoreRule> = listOf(
        IgnoreRule(name = "Git internals", folderName = ".git"),
        IgnoreRule(name = "Gradle internals", folderName = ".gradle"),
        IgnoreRule(name = "Gradle build output", folderName = "build"),
        IgnoreRule(name = "Node modules", folderName = "node_modules"),
        IgnoreRule(name = "IDE project metadata", folderName = ".idea")
    )
}
