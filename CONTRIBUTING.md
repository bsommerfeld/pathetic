# Contributing to Pathetic 🥀

**So you think you are worthy enough to contribute?**

Pathetic didn't crawl to the top of the Java pathfinding food chain by accepting drive-by patches that smell like Sunday-morning vibe coding. If you're here to make it faster, meaner, or less pathetic — pull up a chair. If you're here to add a `Thread.sleep(100)` "just to be safe," kindly close this tab.

> "I sent my first PR to Pathetic and they laughed at my allocations." <br>
> — anonymous contributor, now a better engineer

---

### Before you grace us with your code

| Thing            | Required                        | Why we care                                                         |
|------------------|---------------------------------|---------------------------------------------------------------------|
| **Java**         | 8 or higher                     | Because we still serve mortals on legacy hardware                   |
| **Maven**        | The thing in your project root  | Use `./mvnw` and stop complaining                                   |
| **Git**          | You should know what `rebase` is | If you don't, the [Wiki](https://github.com/bsommerfeld/pathetic/wiki) is over there |
| **Brain**        | Strongly recommended            | We benchmark. We profile. We notice.                                |

Clone the holy grail:

```bash
git clone https://github.com/bsommerfeld/pathetic.git
cd pathetic
```

---

### The Commandments

We are not a democracy. We are a benchmark.

- **No allocations in hot paths.** If your code allocates inside the pathfinding loop, you have personally insulted our [[PrimitiveMinHeap]] and we will know.
- **One PR, one purpose.** Don't bundle a bug fix with a refactor, a refactor with a feature, and a feature with your grocery list. Split it.
- **Cross-platform or it didn't happen.** Windows, macOS, Linux. If you used `\` as a path separator anywhere, leave.
- **Tests are not optional.** New feature? Test it. Touched existing behavior? Update the test. "It works on my machine" is not a unit test.
- **Match the code style.** We do not care that you prefer tabs in your soul. The build verifies formatting. So will we.
- **Discuss the big stuff first.** Want to rewrite the heuristic? Refactor the heap? Add a new movement model? Open an [issue](https://github.com/bsommerfeld/pathetic/issues) first. We'd rather argue with you for an hour than reject 800 lines of PR.

---

### How to actually contribute

1. **Fork it.** Make it yours. Suffer accordingly.
2. **Branch with intent.**
   ```
   feature/perpendicular-jump-correction
   fix/heap-decrease-key-edge-case
   ```
   Not `patch-1`. Not `dev`. Not `final-FINAL-v2`.
3. **Write the change.** Follow the commandments above. Re-read them. We mean it.
4. **Run the tests.**
   ```bash
   ./mvnw test
   ```
   If they're red, you're not done.
5. **Commit like an adult.** We follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):
   ```
   feat: add diagonal pruning for octile heuristic
   fix: correct height penalty sign in composite heuristic
   perf: drop allocation in PathFilter#test
   ```
6. **Update the CHANGELOG.** *Read the next section. We will not say it twice.*
7. **Open the PR** against `trunk`. Describe what changed and why. Reference the issue (`Closes #123`). Pretend the reviewer has never met you — because they haven't.
8. **Respond to review.** Quickly. Politely. With benchmarks if asked.

---

### CHANGELOG.md — yes, you have to touch it

Pathetic ships a [`CHANGELOG.md`](./CHANGELOG.md) at the root of the repo. It is not decorative. It exists so that future maintainers, future users, and future-you can figure out what the hell happened between two versions without spelunking through `git log` like an archaeologist.

**Every PR that changes behavior, performance, or public API updates the CHANGELOG.** No exceptions, no "the maintainer will do it." That maintainer is tired.

The file uses four sections — drop your entry under whichever fits:

- **Added** — new features, new options, new public API
- **Changed** — behavior changes, performance improvements, refactors users can feel
- **Fixed** — bug fixes
- **Removed** — anything that used to exist and now doesn't

Format your entry like the existing ones:

```markdown
- Short, present-tense description of the change [[#PR-number](https://github.com/bsommerfeld/pathetic/pull/PR-number)] by @your-handle
```

Pure internal refactors with zero observable effect (no API change, no perf delta) can skip the CHANGELOG — but if you're unsure, **add the entry**. Over-documenting is a venial sin. Under-documenting is what other libraries do.

---

### Code of Conduct

Yes, we have one. Yes, we mean it.

Be decent. Argue about code, not people. The full text lives at [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md).

---

### Need help?

- Open an issue on [GitHub](https://github.com/bsommerfeld/pathetic/issues).
- Cry or worship in our [Discord](https://discord.gg/zGx9BSzKfJ).
- Check the [Wiki](https://github.com/bsommerfeld/pathetic/wiki) — it exists for a reason.

---

### Acknowledgments

Every benchmark-improving contribution counts. Special thanks to [@Ollie](https://github.com/olijeffers0n) for co-founding the project and to [JetBrains](https://www.jetbrains.com/de-de/community/opensource/) for sponsoring tools to a project literally called *Pathetic*. Legends recognize legends.

---

<div align="center">

**Don't be pathetic. Contribute to Pathetic.** 🥀

</div>
