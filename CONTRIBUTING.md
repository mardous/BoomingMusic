# Contributing to Booming Music

Thank you for your interest in contributing to Booming Music! We value your help in making this the best music player for Android.

To ensure a smooth collaboration process, please review the following guidelines before you start.

## Code of Conduct
By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). We expect all contributors to maintain a respectful and professional environment.

## Communication
The [GitHub Issue Tracker](https://github.com/mardous/BoomingMusic/issues) is the primary channel for bug reports and feature requests. 
- Please do not send emails for these purposes.
- Use GitHub Reactions (👍, 😄, etc.) instead of commenting "+1".

---

## Reporting Issues

### Bug Reports
1. **Search First:** Check both open and closed issues to ensure the bug hasn't been reported.
2. **Reproduce:** Verify the bug on the latest build.
3. **Be Descriptive:** Provide clear steps to reproduce, device information, and screenshots/videos if possible.
4. **Isolate:** One bug per issue. Do not bundle multiple unrelated errors together.

### Feature Requests
Proposals for new features are always welcome! Please open an issue to discuss your idea before starting implementation to ensure it aligns with the project's vision.

---

## Pull Request Process

### 1. Discuss Large Changes
For significant refactors or large features, please open an issue first. This avoids wasted effort if the proposal isn't accepted.

### 2. Specific Focus
Each Pull Request should address a single feature or bug fix. Avoid "mega-PRs" that touch unrelated parts of the app.

### 3. AI Usage Disclosure
> [!IMPORTANT]
> We value human-crafted code. If you used an AI agent (Gemini, Claude, ChatGPT, etc.) to assist in your contribution, you **must** disclose it in the Pull Request using our [PR template](.github/pull_request_template.md).

### 4. UI Contribution Freeze
> [!WARNING]
> We are currently migrating the entire UI to **Jetpack Compose**.
> To prevent merge conflicts, we are **not accepting** PRs that modify, redesign, or add new UI features at this time. Bug fixes for core logic and internal improvements are still highly encouraged!

### 5. Verification
Ensure your code builds correctly using:
```bash
.\gradlew :app:assembleGithubDebug
```

---

## Licensing
By contributing to Booming Music, you agree that your contributions will be licensed under the [GNU General Public License v3.0](LICENSE.txt).
