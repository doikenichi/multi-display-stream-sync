/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import istanbul from "vite-plugin-istanbul";

// https://vite.dev/config/
export default defineConfig(() => {
  return {
    server: {
      host: "0.0.0.0", // Binds to all network interfaces
      port: 3000, // Sets the development server port
    },
    plugins: [
      react(),
      istanbul({
        include: ["src/*"],
        exclude: ["node_modules", "test/"],
        extension: [".ts", ".tsx"],
        requireEnv: true,
      }),
    ],
  };
});
