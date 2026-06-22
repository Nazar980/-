import { useState } from "react";

/* ------------------------------------------------------------------ */
/*  Data                                                               */
/* ------------------------------------------------------------------ */

type FileNode = {
  name: string;
  kind: "dir" | "java" | "json" | "gradle" | "meta";
  note?: string;
  children?: FileNode[];
};

const TREE: FileNode = {
  name: "csce466-mod/",
  kind: "dir",
  children: [
    {
      name: "src/main/java/edu/unl/csce466/",
      kind: "dir",
      children: [
        { name: "ExampleMod.java", kind: "java", note: "@Mod entry point" },
        {
          name: "imgui/",
          kind: "dir",
          children: [
            { name: "ImGuiRenderer.java", kind: "java", note: "singleton — init / render / menu" },
            { name: "ImGuiCall.java", kind: "java", note: "functional interface for draw calls" },
          ],
        },
        {
          name: "mixins/",
          kind: "dir",
          children: [
            { name: "RenderSystemMixin.java", kind: "java", note: "init + per-frame render hook" },
            { name: "MinecraftMixin.java", kind: "java", note: "empty / no-op (kept for compat)" },
          ],
        },
        {
          name: "event/",
          kind: "dir",
          children: [
            { name: "KeyInputHandler.java", kind: "java", note: "InputEvent.Key → toggle menu (NEW)" },
            { name: "ModEvents.java", kind: "java", note: "player interaction test" },
          ],
        },
        { name: "screens/ImGuiScreen.java", kind: "java", note: "unused placeholder" },
      ],
    },
    {
      name: "src/main/resources/",
      kind: "dir",
      children: [
        { name: "META-INF/forge-mixin.json", kind: "json", note: "mixin config (only RenderSystemMixin now)" },
        { name: "pack.mcmeta", kind: "meta", note: "pack_format 48" },
      ],
    },
    { name: "build.gradle", kind: "gradle", note: "Forge 1.21.4-54.1.16 + imgui-java 1.87.5" },
    { name: "settings.gradle", kind: "gradle", note: "rootProject name" },
    { name: "gradle.properties", kind: "gradle", note: "JVM args" },
  ],
};

const FIXES = [
  {
    title: "1 · Убран краш-миксин KeyboardHandlerMixin",
    bad: "@Inject(method = \"onKeyPressed\", remap = false) on net.minecraft.client.Keyboard",
    good: "Удалён. Заменён на Forge-событие InputEvent.Key — никаких хрупких инъекций в ванильные методы.",
    why: "Миксин искал несуществующий метод/маппинг (m_90893_) → Critical injection failure → краш при загрузке класса Keyboard во время Minecraft.<init>.",
  },
  {
    title: "2 · Тоггл меню через Forge событие",
    bad: "Mixin → KeyboardHandler (крашилось)",
    good: "@SubscribeEvent onKeyInput(InputEvent.Key) — нажатие L открывает/закрывает ImGui.",
    why: "Событие InputEvent.Key стабильно приходит на Forge bus и не зависит от обфускации/SRG-имён методов.",
  },
  {
    title: "3 · Интерактивный ImGui (GLFW коллбэки)",
    bad: "imGuiGlfw.init(windowHandle, false) — меню не реагировало на клики",
    good: "imGuiGlfw.init(windowHandle, true) — ImGui ставит коллбэки и чейнит коллбэки Minecraft.",
    why: "Теперь можно кликать кнопки и вводить текст в окнах ImGui, не ломая ввод игры.",
  },
  {
    title: "4 · Дубликат forge-mixin.refmap.json (предыдущий билд)",
    bad: "Task :jar FAILED — duplicate entry",
    good: "duplicatesStrategy = EXCLUDE для jar / processResources.",
    why: "MixGradle и ручной from(...) добавляли refmap дважды; стратегия исключения решает конфликт.",
  },
];

const CONTROLS = [
  { key: "L", action: "Открыть / закрыть меню ImGui" },
  { key: "Click", action: "Взаимодействие с виджетами ImGui" },
  { key: "Right-Click (empty)", action: "Тест-событие ModEvents (если start = true)" },
];

const DEPS = [
  { name: "Minecraft Forge", ver: "1.21.4-54.1.16" },
  { name: "imgui-java", ver: "1.87.5 (binding / lwjgl3 + natives)" },
  { name: "LWJGL", ver: "3.3.3 (glfw, opengl)" },
  { name: "Mixin", ver: "0.8.5 (annotation processor) / 0.8.7 (runtime)" },
  { name: "Java", ver: "21 (toolchain)" },
];

const FLOW = [
  { step: "RenderSystem.initRenderer()", desc: "TAIL-инъекция создаёт ImGui-контекст и привязывает GLFW-окно (glfwGetCurrentContext)." },
  { step: "glfwSet*Callback", desc: "ImGui устанавливает коллбэки ввода и сохраняет старые коллбэки Minecraft." },
  { step: "RenderSystem.flipFrame()", desc: "HEAD-инъекция вызывает ImGuiRenderer.render() каждый кадр." },
  { step: "InputEvent.Key", desc: "Forge шлёт событие → нажатие L тогглит menuVisible." },
  { step: "ImGui.newFrame() / render()", desc: "Отрисовка меню и draw-вызовов поверх кадра Minecraft." },
];

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */

const COLORS: Record<FileNode["kind"], string> = {
  dir: "text-emerald-300",
  java: "text-orange-300",
  json: "text-amber-300",
  gradle: "text-sky-300",
  meta: "text-zinc-400",
};

function Tree({ node, depth = 0 }: { node: FileNode; depth?: number }) {
  const isDir = node.kind === "dir";
  return (
    <div>
      <div
        className="flex flex-wrap items-baseline gap-x-2 py-0.5"
        style={{ paddingLeft: depth * 18 }}
      >
        <span className="select-none text-zinc-600">{isDir ? "▸" : "·"}</span>
        <span className={`font-mono text-sm ${COLORS[node.kind]}`}>{node.name}</span>
        {node.note && <span className="font-mono text-xs text-zinc-500"># {node.note}</span>}
      </div>
      {node.children?.map((c) => (
        <Tree key={c.name} node={c} depth={depth + 1} />
      ))}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  App                                                                */
/* ------------------------------------------------------------------ */

export default function App() {
  const [tab, setTab] = useState<"fixes" | "structure" | "flow">("fixes");

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200 antialiased">
      {/* glow background */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute -top-40 left-1/2 h-96 w-[42rem] -translate-x-1/2 rounded-full bg-emerald-600/20 blur-3xl" />
        <div className="absolute top-1/3 -right-32 h-72 w-72 rounded-full bg-lime-500/10 blur-3xl" />
      </div>

      <div className="relative mx-auto max-w-5xl px-5 py-12 sm:px-8">
        {/* Header */}
        <header className="mb-10">
          <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-300">
            <span className="h-2 w-2 animate-pulse rounded-full bg-emerald-400" />
            Minecraft 1.21.4 · Forge · ImGui Overlay
          </div>
          <h1 className="text-4xl font-extrabold tracking-tight text-white sm:text-5xl">
            CSCE466 Mod
          </h1>
          <p className="mt-3 max-w-2xl text-zinc-400">
            Оверлей <span className="text-emerald-300">Dear ImGui</span> поверх Minecraft.
            Документация проекта, исправления краша и схема рендера.
          </p>
          <div className="mt-5 flex flex-wrap gap-2">
            <span className="rounded-md bg-zinc-800/80 px-2.5 py-1 font-mono text-xs text-zinc-300">forge: 1.21.4-54.1.16</span>
            <span className="rounded-md bg-zinc-800/80 px-2.5 py-1 font-mono text-xs text-zinc-300">imgui-java: 1.87.5</span>
            <span className="rounded-md bg-zinc-800/80 px-2.5 py-1 font-mono text-xs text-zinc-300">java: 21</span>
            <span className="rounded-md bg-zinc-800/80 px-2.5 py-1 font-mono text-xs text-zinc-300">modid: examplemod</span>
          </div>
        </header>

        {/* Controls */}
        <section className="mb-10">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-zinc-500">Управление</h2>
          <div className="grid gap-3 sm:grid-cols-3">
            {CONTROLS.map((c) => (
              <div key={c.key} className="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
                <kbd className="inline-block rounded-md border border-emerald-500/40 bg-emerald-500/10 px-3 py-1.5 font-mono text-sm font-bold text-emerald-300 shadow-inner">
                  {c.key}
                </kbd>
                <p className="mt-2 text-sm text-zinc-400">{c.action}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Tabs */}
        <div className="mb-4 flex gap-1 rounded-lg border border-zinc-800 bg-zinc-900/60 p-1">
          {([
            ["fixes", "Исправления краша"],
            ["structure", "Структура проекта"],
            ["flow", "Как работает рендер"],
          ] as const).map(([id, label]) => (
            <button
              key={id}
              onClick={() => setTab(id)}
              className={`flex-1 rounded-md px-3 py-2 text-sm font-medium transition ${
                tab === id
                  ? "bg-emerald-500/20 text-emerald-300"
                  : "text-zinc-400 hover:text-zinc-200"
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        {/* Tab content */}
        <section className="mb-10 rounded-xl border border-zinc-800 bg-zinc-900/40 p-5 sm:p-6">
          {tab === "fixes" && (
            <div className="space-y-5">
              {FIXES.map((f) => (
                <div key={f.title} className="rounded-lg border border-zinc-800 bg-zinc-950/50 p-4">
                  <h3 className="font-semibold text-white">{f.title}</h3>
                  <div className="mt-3 space-y-2 text-sm">
                    <div className="flex gap-2">
                      <span className="mt-0.5 font-mono text-xs font-bold text-red-400">WAS</span>
                      <code className="font-mono text-zinc-400">{f.bad}</code>
                    </div>
                    <div className="flex gap-2">
                      <span className="mt-0.5 font-mono text-xs font-bold text-emerald-400">NOW</span>
                      <code className="font-mono text-zinc-300">{f.good}</code>
                    </div>
                  </div>
                  <p className="mt-3 border-l-2 border-zinc-700 pl-3 text-sm text-zinc-500">{f.why}</p>
                </div>
              ))}
            </div>
          )}

          {tab === "structure" && (
            <div className="overflow-x-auto">
              <Tree node={TREE} />
            </div>
          )}

          {tab === "flow" && (
            <ol className="space-y-4">
              {FLOW.map((f, i) => (
                <li key={f.step} className="flex gap-4">
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-500/15 font-mono text-sm font-bold text-emerald-300">
                    {i + 1}
                  </div>
                  <div>
                    <code className="font-mono text-sm text-amber-300">{f.step}</code>
                    <p className="mt-1 text-sm text-zinc-400">{f.desc}</p>
                  </div>
                </li>
              ))}
            </ol>
          )}
        </section>

        {/* Dependencies */}
        <section className="mb-10">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-zinc-500">Зависимости</h2>
          <div className="grid gap-2 sm:grid-cols-2">
            {DEPS.map((d) => (
              <div key={d.name} className="flex items-center justify-between rounded-lg border border-zinc-800 bg-zinc-900/60 px-4 py-3">
                <span className="text-sm text-zinc-300">{d.name}</span>
                <span className="font-mono text-xs text-zinc-500">{d.ver}</span>
              </div>
            ))}
          </div>
        </section>

        {/* Build */}
        <section className="mb-10 rounded-xl border border-zinc-800 bg-zinc-900/40 p-5 sm:p-6">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-zinc-500">Сборка</h2>
          <div className="space-y-3">
            <div>
              <p className="mb-1 text-xs text-zinc-500">Собрать мод (reobf → build/libs/csce466-mod-1.0.jar):</p>
              <pre className="overflow-x-auto rounded-lg border border-zinc-800 bg-zinc-950 p-3 font-mono text-sm text-emerald-300">./gradlew clean build</pre>
            </div>
            <div>
              <p className="mb-1 text-xs text-zinc-500">Запустить тестовый клиент из IDE:</p>
              <pre className="overflow-x-auto rounded-lg border border-zinc-800 bg-zinc-950 p-3 font-mono text-sm text-emerald-300">./gradlew runClient</pre>
            </div>
            <p className="text-sm text-zinc-500">
              Готовый <code className="font-mono text-zinc-400">jar</code> кладётся в папку{" "}
              <code className="font-mono text-zinc-400">mods</code> Forge 1.21.4 вместе с нативными jar-in-jar imgui-java.
            </p>
          </div>
        </section>

        <footer className="border-t border-zinc-800 pt-6 text-center text-xs text-zinc-600">
          CSCE466 · edu.unl.csce466 · ImGui overlay for Minecraft 1.21.4 (Forge)
        </footer>
      </div>
    </div>
  );
}
