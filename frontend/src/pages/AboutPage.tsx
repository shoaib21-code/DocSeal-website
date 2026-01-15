export default function AboutPage() {
  return (
    <section className="mx-auto max-w-5xl px-4 py-10">
      <div className="relative overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="h-1 w-full bg-gradient-to-r from-indigo-500 via-sky-500 to-teal-400" />

        <div className="prose prose-slate max-w-none p-6 sm:p-10">
          <h1 className="mb-2 text-3xl font-extrabold tracking-tight">
            <span className="bg-gradient-to-r from-indigo-600 to-sky-500 bg-clip-text text-transparent">
              About DocSeal
            </span>
          </h1>

          <p className="!mt-0 text-slate-600">
            <strong>Trust shouldn’t be hard.</strong> We built <strong>DocSeal</strong> to make
            professional-grade document signing feel effortless. Behind the scenes it’s real
            public-key cryptography; up front, it’s a clean workflow anyone can follow.
          </p>

          {/* Feature grid */}
          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            <div className="group rounded-xl border border-slate-200 p-4 transition hover:shadow">
              <div className="mb-1 flex items-center gap-2 font-semibold">
                <span>Create keys you control</span>
              </div>
              <p className="m-0 text-sm text-slate-600">
                Generate RSA 3072/4096 key pairs locally on the server.
              </p>
            </div>

            <div className="group rounded-xl border border-slate-200 p-4 transition hover:shadow">
              <div className="mb-1 flex items-center gap-2 font-semibold">
                <span>Get a certificate</span>
              </div>
              <p className="m-0 text-sm text-slate-600">
                Standards-based CSR → certificate → chain, ready for distribution.
              </p>
            </div>

            <div className="group rounded-xl border border-slate-200 p-4 transition hover:shadow">
              <div className="mb-1 flex items-center gap-2 font-semibold">
                <span>Sign your file</span>
              </div>
              <p className="m-0 text-sm text-slate-600">
                Produce a portable signature bundle that travels with the document.
              </p>
            </div>

            <div className="group rounded-xl border border-slate-200 p-4 transition hover:shadow">
              <div className="mb-1 flex items-center gap-2 font-semibold">
                <span>Verify anywhere</span>
              </div>
              <p className="m-0 text-sm text-slate-600">
                Recipients can confirm integrity and origin.
              </p>
            </div>
          </div>

          {/* Security callout */}
          <div className="mt-6 rounded-xl border border-amber-200 bg-amber-50 p-4 text-amber-900">
            <p className="m-0 text-sm">
              <strong>Security first:</strong> Private keys are stored only on the server and are
              never returned to the client.
            </p>
          </div>

          {/* Tech stack chips */}
          <h3 className="mt-8">Tech Stack</h3>
          <div className="mt-2 flex flex-wrap gap-2">
            <span className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-sm">
              Backend: Spring Boot 3
            </span>
            <span className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-sm">
              JPA
            </span>
            <span className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-sm">
              PostgreSQL
            </span>
            <span className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-sm">
              Frontend: React + Vite
            </span>
            <span className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-sm">
              Tailwind CSS
            </span>
          </div>

       
          <h3 className="mt-8">About the Project</h3>
          <div className="mt-3 rounded-2xl border border-slate-200 bg-slate-50 p-5">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-center gap-4">
                <div className="grid h-12 w-12 place-items-center rounded-2xl bg-slate-900 text-white font-semibold">
                  SA
                </div>
                <div>
                  <div className="text-base font-semibold text-slate-900">
                    DocSeal — Master’s Major Project
                  </div>
                  <div className="text-sm text-slate-600">
                    Built by <strong>Shoaibuddin Ahmed Mohammed</strong> as part of a Master’s major project.
                  </div>
                </div>
              </div>

              <div className="flex flex-wrap gap-2">
              
                <a
                  href="#"
                  className="no-underline inline-flex items-center rounded-full border border-slate-200 bg-white px-3 py-1 text-sm text-slate-700 hover:bg-slate-100"
                >
                  GitHub
                </a>
                <a
                  href="#"
                  className="no-underline inline-flex items-center rounded-full border border-slate-200 bg-white px-3 py-1 text-sm text-slate-700 hover:bg-slate-100"
                >
                  LinkedIn
                </a>
                <a
                  href="mailto:yshoaibuddin.ahmed21@gmail.com"
                  className="no-underline inline-flex items-center rounded-full border border-slate-200 bg-white px-3 py-1 text-sm text-slate-700 hover:bg-slate-100"
                >
                  Email
                </a>
              </div>
            </div>

            <div className="mt-4 grid gap-3 sm:grid-cols-3">
              <div className="rounded-xl border border-slate-200 bg-white p-4">
                <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Objective
                </div>
                <div className="mt-1 text-sm text-slate-700">
                  Make secure document signing simple, reliable, and easy to verify.
                </div>
              </div>

              <div className="rounded-xl border border-slate-200 bg-white p-4">
                <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  What it demonstrates
                </div>
                <div className="mt-1 text-sm text-slate-700">
                  Public-key cryptography, certificate workflows, and end-to-end web app design.
                </div>
              </div>

              <div className="rounded-xl border border-slate-200 bg-white p-4">
                <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Security note
                </div>
                <div className="mt-1 text-sm text-slate-700">
                  Private keys remain server-side and are never sent to the client.
                </div>
              </div>
            </div>
          </div>


          {/* CTA */}
          <div className="mt-8">
            <a
              href="/"
              className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-900 px-5 py-2.5 text-sm font-medium text-white shadow hover:-translate-y-0.5 hover:shadow-md active:translate-y-0 transition"
            >
              Get started
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-4 w-4"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path strokeWidth="1.5" d="M13.5 4.5L21 12l-7.5 7.5M21 12H3" />
              </svg>
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}
