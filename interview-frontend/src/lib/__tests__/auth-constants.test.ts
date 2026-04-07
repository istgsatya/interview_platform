import { describe, expect, it } from "vitest";
import { TOKEN_KEY } from "../auth-constants";

describe("auth constants", () => {
  it("keeps token key stable", () => {
    expect(TOKEN_KEY).toBe("interview_token");
  });
});
