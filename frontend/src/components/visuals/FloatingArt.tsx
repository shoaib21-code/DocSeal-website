import { motion } from 'framer-motion'

export default function FloatingArt() {
  return (
    <motion.img
      src="/hero.png"
      alt="Signing Illustration"
      className="w-full max-w-[520px] drop-shadow-xl"
      animate={{ y: [0, -8, 0] }}
      transition={{ duration: 5, repeat: Infinity, ease: 'easeInOut' }}
    />
  )
}
