rootProject.name = "public"


include(":ExtUtils")
include(":ZulrahSwapper")
include(":NightmareSwapper")
include(":ThievingAutoClicker")
include(":AutoBlisterwoodCutter")
include(":Barrows")

for (project in rootProject.children) {
    project.apply {
        projectDir = file(name)
        buildFileName = "$name.gradle.kts"

        require(projectDir.isDirectory) { "Project '${project.path} must have a $projectDir directory" }
        require(buildFile.isFile) { "Project '${project.path} must have a $buildFile build script" }
    }
}
