import { useState } from "react";

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
            { name: "ImGuiRenderer.java", kind: "java", note: "singleton — init / font atlas / render / menu" },
            { name: "ImGuiCall.java", kind: "java", note: "functional interface for draw calls" },
          ],
        },
        {
          name: "mixins/",
          kind: "dir",
          children: [
            { name: "RenderSystemMixin.java", kind: "java", note: "init + per-frame render hook (try-catch)" },
            { name: "MinecraftMixin.java", kind: "java", note: "empty / no-op (kept for compat)" },
          ],
        },
        {
          name: "client/",
          kind: "dir",
          children: [
            { name: "ClientInputEvents.java", kind: "java", note: "InputEvent.Key -> toggle menu L" },
          ],
        },
        {
          name: "event/",
          kind: "dir",
          children: [
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
        { name: "META-INF/forge-mixin.json", kind: "json", note: "mixin config (only RenderSystemMixin)" },
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
    id: 1,
    title: "Краш: KeyboardHandlerMixin → Critical injection failure",
    bad: "Mixin с remap=false искал метод onKeyPressed/m_90893_ — такого нет в 1.21.4",
    good: "Миксин УДАЛЁН. Заменён на Forge-событие InputEvent.Key (ClientInputEvents.java)",
    why: "Миксин не мог найти целевой метод → MixinTransformerError → Minecraft.<init> крашился при загрузке Keyboard.",
  },
  {
    id: 2,
    title: "Ассерт: Font Atlas not built при newFrame()",
    bad: "flipFrame вызывался до полной инициализации шрифтового атласа ImGui",
    good: "Принудительный fontAtlas.build() в init() + проверка fontAtlasBuilt перед каждым render()",
    why: "ImGui ассертит если newFrame() вызвать до того как GL-текстура шрифтов создана рендерером.",
  },
  {
    id: 3,
    title: "try-catch в flipFrame хуке",
    bad: "Любой ImGui exception крашил всю игру",
    good: "onFlipFrame обёрнут в try-catch — ImGui ошибка не роняет Minecraft",
    why: "Безопасность: оверлей не должен убивать игру при ошибке рендера.",
  },
  {
    id: 4,
    title: "Дубликат forge-mixin.refmap.json",
    bad: "Task :jar FAILED — duplicate entry без стратегии",
    good: "duplicatesStrategy = EXCLUDE для jar и processResources",
    why: "MixinGradle и ручной from() добавляли refmap дважды.",
  },
];

const CONTROLS = [
  { key: "L", action: "Открыть / закрыть меню ImGui" },
  { key: "Click", action: "Клик по виджетам ImGui (кнопки и т.д.)" },
  { key: "Right-Click (empty)", action: "Тест-событие ModEvents (если start = true)" },
];



const FLOW = [
  { step: "RenderSystem.initRenderer()", desc: "TAIL-инъекция: создаём ImGui-контекст, привязываем GLFW-окно, строим шрифтовой атлас." },
  { step: "glfwSet*Callback (true)", desc: "ImGui устанавливает свои GLFW-коллбэки и чейнит старые коллбэки Minecraft." },
  { step: "fontAtlas.build()", desc: "Принудительная сборка текстуры шрифтов, чтобы первый newFrame() не ассертил." },
  { step: "RenderSystem.flipFrame()", desc: "HEAD-инъекция (try-catch): вызывает ImGuiRenderer.render() каждый кадр." },
  { step: "InputEvent.Key → L", desc: "Forge-событие тогглит menuVisible без хрупких миксинов." },
  { step: "ImGui.newFrame() / render()", desc: "Рисуем меню и draw-вызовы поверх кадра Minecraft." },
];

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
      <div className="flex flex-wrap items-baseline gap-x-2 py-0.5" style={{ paddingLeft: depth * 18 }}>
        <span className="select-none text-zinc-600">{isDir ? "▸" : "·"}</span>
        <span className={`font-mono text-sm ${COLORS[node.kind]}`}>{node.name}</span>
        {node.note && <span className="font-mono text-xs text-zinc-500"># {node.note}</span>}
      </div>
      {node.children?.map((c) => <Tree key={c.name} node={c} depth={depth + 1} />)}
    </div>
  );
}

export default function App() {
  const [tab, setTab] = useState<"fixes" | "structure" | "flow">("fixes");

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-200 antialiased">
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute -top-40 left-1/2 h-96 w-[42rem] -translate-x-1/2 rounded-full bg-emerald-600/20 blur-3xl" />
      </div>

      <div className="relative mx-auto max-w-5xl px-5 py-12 sm:px-8">
        <header className="mb-10">
          <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-300">
            <span className="h-2 w-2 animate-pulse rounded-full bg-emerald-400" />
            Minecraft 1.21.4 · Forge · ImGui Overlay
          </div>
          <h1 className="text-4xl font-extrabold tracking-tight text-white sm:text-5xl">CSCE466 Mod</h1>
          <p className="mt-3 max-w-2xl text-zinc-400">
            Оверлей <span className="text-emerald-300">Dear ImGui</span> поверх Minecraft. Все краши исправлены.
          </p>
          <div className="mt-5 flex flex-wrap gap-2">
            {["forge: 1.21.4-54.1.16", "imgui-java: 1.87.5", "java: 21", "modid: examplemod"].map((t) => (
              <span key={t} className="rounded-md bg-zinc-800/80 px-2.5 py-1 font-mono text-xs text-zinc-300">{t}</span>
            ))}
          </div>
        </header>

        <section className="mb-10">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-zinc-500">Управление</h2>
          <div className="grid gap-3 sm:grid-cols-3">
            {CONTROLS.map((c) => (
              <div key={c.key} className="rounded-xl border border-zinc-800 bg-zinc-900/60 p-4">
                <kbd className="inline-block rounded-md border border-emerald-500/40 bg-emerald-500/10 px-3 py-1.5 font-mono text-sm font-bold text-emerald-300">{c.key}</kbd>
                <p className="mt-2 text-sm text-zinc-400">{c.action}</p>
              </div>
            ))}
          </div>
        </section>

        <div className="mb-4 flex gap-1 rounded-lg border border-zinc-800 bg-zinc-900/60 p-1">
          {([["fixes", "Исправления"], ["structure", "Структура"], ["flow", "Рендер"]] as const).map(([id, label]) => (
            <button
              key={id}
              onClick={() => setTab(id)}
              className={`flex-1 rounded-md px-3 py-2 text-sm font-medium transition ${tab === id ? "bg-emerald-500/20 text-emerald-300" : "text-zinc-400 hover:text-zinc-200"}`}
            >
              {label}
            </button>
          ))}
        </div>

        <section className="mb-10 rounded-xl border border-zinc-800 bg-zinc-900/40 p-5 sm:p-6">
          {tab === "fixes" && (
            <div className="space-y-5">
              {FIXES.map((f) => (
                <div key={f.id} className="rounded-lg border border-zinc-800 bg-zinc-950/50 p-4">
                  <h3 className="font-semibold text-white">{f.id}. {f.title}</h3>
                  <div className="mt-3 space-y-2 text-sm">
                    <div className="flex gap-2"><span className="mt-0.5 font-mono text-xs font-bold text-red-400">WAS</span><code className="font-mono text-zinc-400">{f.bad}</code></div>
                    <div className="flex gap-2"><span className="mt-0.5 font-mono text-xs font-bold text-emerald-400">NOW</span><code className="font-mono text-zinc-300">{f.good}</code></div>
                  </div>
                  <p className="mt-3 border-l-2 border-zinc-700 pl-3 text-sm text-zinc-500">{f.why}</p>
                </div>
              ))}
            </div>
          )}
          {tab === "structure" && <Tree node={TREE} />}
          {tab === "flow" && (
            <ol className="space-y-4">
              {FLOW.map((f, i) => (
                <li key={f.step} className="flex gap-4">
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-500/15 font-mono text-sm font-bold text-emerald-300">{i + 1}</div>
                  <div>
                    <code className="font-mono text-sm text-amber-300">{f.step}</code>
                    <p className="mt-1 text-sm text-zinc-400">{f.desc}</p>
                  </div>
                </li>
              ))}
            </ol>
          )}
        </section>

        <section className="mb-10">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-zinc-500">Сборка</h2>
          <div className="space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
            <div>
              <p className="mb-1 text-xs text-zinc-500">Собрать мод:</p>
              <pre className="overflow-x-auto rounded-lg border border-zinc-800 bg-zinc-950 p-3 font-mono text-sm text-emerald-300">./gradlew clean build</pre>
            </div>
            <div>
              <p className="mb-1 text-xs text-zinc-500">Тестовый клиент:</p>
              <pre className="overflow-x-auto rounded-lg border border-zinc-800 bg-zinc-950 p-3 font-mono text-sm text-emerald-300">./gradlew runClient</pre>
            </div>
          </div>
        </section>

        <footer className="border-t border-zinc-800 pt-6 text-center text-xs text-zinc-600">
          CSCE466 · edu.unl.csce466 · Minecraft 1.21.4 Forge + ImGui
        </footer>
      </div>
    </div>
  );
}
