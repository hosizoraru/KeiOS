# ---------- KeiOS app R8 rules ----------
#
# Release adds these rules to AGP's optimized defaults. Module-owned rules live in
# src/main/keepRules; libraries supply their own serialization and reflection rules.

# Keep the *names* of throwables, and only the names.
#
# The app shows `javaClass.simpleName` to the teacher in about twenty places, and two of them are not
# cosmetic: FeedbackIssueRepository and FeedbackIssueViewModel put it in the body of the GitHub issue
# they file. Obfuscated, that reads `a43` — measured, not guessed: DownloadSizeMismatchException maps to
# exactly that in the release build. A submitted report naming `a43` cannot be triaged, and no mapping
# file is attached to it.
#
# Preserve diagnostic names while allowing unused classes to disappear and live classes to optimize.
-keep,allowshrinking,allowoptimization class * extends java.lang.Throwable
