export default function AnimatedGradient() {
  return (
    <div aria-hidden className="pointer-events-none absolute inset-0 overflow-hidden">
      <div className="animate-gradient absolute -left-1/3 -top-1/3 h-[120vh] w-[120vw] rounded-[50%]
                      bg-[radial-gradient(60%_60%_at_50%_40%,#2563EB22,transparent),radial-gradient(35%_35%_at_60%_60%,#F59E0B1f,transparent),radial-gradient(40%_40%_at_40%_60%,#10B9811f,transparent)]" />
    </div>
  )
}
