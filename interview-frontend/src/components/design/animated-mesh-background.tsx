"use client";

import { motion } from "framer-motion";

const blobs = [
  {
    className: "mesh-blob h-[420px] w-[420px] bg-cyan-500/55 top-[-8%] left-[-8%]",
    animate: { x: [0, 65, -20, 0], y: [0, 25, 55, 0], scale: [1, 1.14, 0.95, 1] },
  },
  {
    className: "mesh-blob h-[390px] w-[390px] bg-indigo-500/55 top-[15%] right-[-7%]",
    animate: { x: [0, -50, 15, 0], y: [0, 40, -15, 0], scale: [1, 0.9, 1.1, 1] },
  },
  {
    className: "mesh-blob h-[360px] w-[360px] bg-emerald-500/40 bottom-[-12%] left-[24%]",
    animate: { x: [0, 25, -35, 0], y: [0, -45, 20, 0], scale: [1, 1.08, 0.98, 1] },
  },
];

export function AnimatedMeshBackground() {
  return (
    <div className="mesh-bg">
      {blobs.map((blob, index) => (
        <motion.div
          key={blob.className}
          className={blob.className}
          animate={blob.animate}
          transition={{ duration: 14 + index * 3, repeat: Infinity, ease: "easeInOut" }}
        />
      ))}
      <motion.div
        className="absolute inset-0 bg-[radial-gradient(circle_at_50%_40%,rgba(15,23,42,0)_0%,rgba(2,6,23,0.88)_68%)]"
        animate={{ opacity: [0.8, 1, 0.86, 1] }}
        transition={{ duration: 9, repeat: Infinity, ease: "easeInOut" }}
      />
    </div>
  );
}
