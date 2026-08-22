# PostApp

An Android app that lists posts from [JSONPlaceholder](https://jsonplaceholder.typicode.com/posts), opens each one on a detail screen, and lets you edit its title and description or swipe a row away to delete it. The UI implements a supplied design spec built on a design system called *Organic* — a cream ground, terracotta and sage accents, Caprasimo display type over Figtree, and heavily rounded geometry.

Views are XML — RecyclerView, fragments, ViewBinding — not Compose, as specified.

---

## Running it

Open the project in Android Studio and run the `app` configuration, or from the command line:

```bash
./gradlew :app:assembleDebug
```

**You need a `local.properties` with your SDK path.** Android Studio writes it on first open; from the CLI, create it yourself:

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

It is the only file the build needs that is not in the repo (it is gitignored because the path is machine-specific).

Verified on JDK 21 and JDK 26, Gradle 8.13, AGP 8.13.2. `minSdk` is 33 (Android 13); it is a single value in `gradle/libs.versions.toml` if you want to go lower.

```bash
./gradlew :domain:test            # pure JVM tests, no Android
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug          # warningsAsErrors is on; it passes clean
```

---

## Module structure

```
:app  ──────────►  :data  ──────────►  :domain
  │                                       ▲
  └───────────────────────────────────────┘
```

| Module | Type | Holds |
|---|---|---|
| `:domain` | `java-library` + Kotlin JVM | `Post`, `PostRepository` (interface), `GetPostsUseCase`, `AppResult`/`AppError`, `DispatcherProvider`, and the pure `PostListOps` transforms |
| `:data` | Android library | `PostDto`, `PostApi`, `PostMapper`, `PostRepositoryImpl`, Retrofit/OkHttp/Moshi wiring |
| `:app` | Android application | Fragments, ViewModels, adapters, resources, DI entry point |

**Why separate Gradle modules rather than packages.** `:domain` deliberately does not apply the Android plugin, so the Android SDK is not on its classpath at all. That turns the dependency rule from a convention into a compiler error: you *cannot* accidentally reference `android.util.Log` or a `Context` from domain code. As a bonus its tests run as plain JVM tests with no Robolectric and no emulator.

Its only dependencies are `kotlinx-coroutines-core` and the `javax.inject` annotations jar — the latter so constructor injection works there without dragging Hilt (and therefore Android) in.

---

## Architecture

```
PostApi ──► PostDto ──► PostMapper ──► Post ──► GetPostsUseCase ──► PostsViewModel ──► Fragment
└──────────── :data ──────────────┘   └────────── :domain ─────────┘  └───── :app ─────┘
```

**Network models never leave `:data`.** `PostDto` is Moshi's shape of the JSON. `PostMapper` converts it to the domain `Post`, and everything above the repository interface speaks `Post` only. The mapper also normalises whitespace, because the API stores titles and bodies with embedded newlines that would otherwise collapse the single-line row title.

**Errors are translated at the boundary too.** `PostRepositoryImpl` catches `IOException`, `HttpException` and `JsonDataException` and returns a typed `AppError`. Nothing above `:data` has to know an HTTP client is involved, and swapping Retrofit out would not touch the presentation layer.

**The repository is stateless** — one `suspend fun getPosts(): AppResult<List<Post>>`. JSONPlaceholder accepts `PUT`/`DELETE` and echoes back a 200 without persisting anything, so there is no remote state worth writing back to or caching. Edits and deletes therefore live in the ViewModel and reset on app restart, which is exactly what the design prototype does.

---

## Testing strategy

No test cases yet — the structure is in place for them.

The seams:

- **`PostRepository`** is a stateless `:domain` interface bound with Hilt `@Binds`. `FakePostRepository` (in `app/src/test`) implements it in a dozen lines with `returns(posts)` / `fails(error)` helpers and a `callCount`. There is no cache behaviour to stub.
- **`DispatcherProvider`** is injected, so `TestDispatcherProvider` swaps in a `StandardTestDispatcher`.
- **`MainDispatcherRule`** replaces `Dispatchers.Main` so `viewModelScope` runs off-device.
- **ViewModels** take their dependencies by constructor and import nothing from `android.*`.
- **`PostListOps`** are pure functions, so update/delete logic is testable with no ViewModel, no coroutines and no mocking at all.

What I'd cover: `PostListOps` (update, delete, no-op identity, missing id) as plain JVM tests in `:domain`; `PostsViewModel` state transitions (loading → content, loading → error, content → empty after deleting the last post, edit produces a new instance for one id only) with Turbine; `PostDetailViewModel` editor lifecycle (start, draft, cancel discards, blank commit returns null); and `PostMapper` whitespace normalisation and `imageIndex` assignment.
