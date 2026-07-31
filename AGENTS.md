# Booming Music Agent Guidelines

## Project Information
- **Project Name:** Booming Music
- **Application ID:** `com.mardous.booming`
- **Build Command:** `./gradlew :app:assembleGithubDebug`

## App Description
Booming Music is a modern, feature-rich music player for Android, designed to provide a high-quality audio experience with advanced playback features and a customizable interface.

## Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3) with legacy View support
- **Media Playback:** Media3 (ExoPlayer)
- **Dependency Injection:** Koin
- **Local Database:** Room
- **Networking:** Ktor
- **Image Loading:** Coil
- **Navigation:** Jetpack Navigation
- **Concurrency:** Kotlin Coroutines & Flow
- **Data Serialization:** Kotlinx Serialization
- **Metadata Handling:** TagLib & Jaudiotagger

## Rules for AI Agents

1. **Documentation and Workflows:** The agent must not modify documentation files like `README.md` or GitHub templates/workflows unless explicitly requested.
2. **Git Integrity:** The agent must not execute commands that compromise the Git branch or alter history unless specifically demanded by the human user. In such cases, the user assumes responsibility for reviewing the changes.
3. **Commits and Pushing:** The agent must not perform commits or push changes unless explicitly instructed to do so.
4. **Localization Strings:** When modifying or adding strings, only use the main strings file (located in the `values` folder). The agent is prohibited from directly modifying strings files for other languages.
5. **Commit Message Format:** Commits must follow the format: `type(scope): short description`. 
   - *Example:* `feat(player): add blur radius setting`
6. **Code Comments:** Add comments only for non-obvious or complex code. Avoid commenting simple lines to keep the code readable and non-redundant.
7. **Human Collaboration:** Always follow the rules and guidelines provided by the human collaborator.
8. **Performance and Battery Life:** Prioritize performance and battery consumption in all changes. Always consider the impact on the user experience. The agent must inform the human collaborator if a requested change might compromise these aspects.
9. **No Assumptions:** If in doubt, the agent must always consult the human collaborator instead of making assumptions about the code's logic or functionality.
10. **Version Number:** It is strictly forbidden for the agent to alter the application's version number. This task is reserved for the primary development team.
11. **Contributors and Collaborators:** The agent must not modify any information about project contributors or collaborators.
12. **Test Configuration:** Do not attempt to add test configurations unless they are already present in the project or specifically requested by the human user.
13. **Language:** All code, comments, and documentation produced by the agent must be in English.
14. **UI Development:** New UI changes or additions must prioritize Jetpack Compose. The project is currently migrating from the legacy View/XML system to Compose.
