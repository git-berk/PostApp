# PostApp

An Android app that lists posts from [JSONPlaceholder](https://jsonplaceholder.typicode.com/posts), opens each one on a detail screen, and lets you edit its title and description or swipe a row away to delete it.

Views are XML — RecyclerView, fragments, ViewBinding.

---

## Module structure

```
:app  ──────────►  :data  ──────────►  :domain
  │                                       ▲
  └───────────────────────────────────────┘
```

| Module | Type | Holds |
|---|---|---|
| `:domain` | `java-library` + Kotlin JVM | `Post`, `PostRepository` (interface), `SyncState`, `AppError`, `DispatcherProvider`, and the pure `PostListOps` transforms |
| `:data` | Android library | `PostDto`, `PostApi`, `PostMapper`, `PostRepositoryImpl`, Retrofit/OkHttp/Moshi wiring |
| `:app` | Android application | Fragments, ViewModels, UI state types, adapters, resources, DI entry point |

## Testing strategy

77 JVM tests: `:domain` 13, `:data` 18, `:app` 46. Run them with `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest`.

The seams that make that possible:

- **`PostRepository`** is a `:domain` interface bound with Hilt `@Binds`. `FakePostRepository` backs it with plain `MutableStateFlow`s and runs the real `PostListOps` for mutations, so ViewModel tests exercise actual behaviour and only the network is faked. Helpers (`refreshReturns`, `refreshFails`, `refreshNeverCompletes`, `setLoaded`) drive it into any state, including a fetch that never returns.
- **`DispatcherProvider`** is injected, so `TestDispatcherProvider` swaps in a `StandardTestDispatcher`.
- **`MainDispatcherRule`** replaces `Dispatchers.Main` so `viewModelScope` runs off-device.
- **ViewModels** take their dependencies by constructor and import nothing from `android.*`.
- **`PostListOps`** are pure functions, so update/delete logic is testable with no ViewModel, no coroutines and no mocking at all.
- **No mocking library.** Everything is hand-written fakes and pure functions; `PostRepositoryImpl`'s own tests use a gated `FakePostApi` (a `CompletableDeferred`) to make the concurrency cases deterministic under `runTest`'s virtual clock.
