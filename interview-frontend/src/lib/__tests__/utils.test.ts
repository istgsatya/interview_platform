import { describe, expect, it } from "vitest";
import { cn } from "../utils";

describe("cn", () => {
  it("merges tailwind classes and resolves conflicts", () => {
    const result = cn("px-2", "px-4", "text-sm");

    expect(result).toContain("px-4");
    expect(result).toContain("text-sm");
    expect(result).not.toContain("px-2");
  });

  it("ignores falsy conditional classes", () => {
    const isActive = false;
    const result = cn("base", isActive && "active", undefined, null, false);

    expect(result).toBe("base");
  });
});
