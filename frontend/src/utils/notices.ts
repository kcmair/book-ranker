import { ApiRequestError } from "../api/client";
import type { Notice } from "./appTypes";

export async function withNotice(
  setLoading: (key: string) => void,
  setNotice: (notice: Notice) => void,
  loadingKey: string,
  task: () => Promise<void>
) {
  setLoading(loadingKey);
  setNotice(null);

  try {
    await task();
  } catch (error) {
    setNotice(noticeFromError(error));
  } finally {
    setLoading("");
  }
}

function noticeFromError(error: unknown): Notice {
  if (error instanceof ApiRequestError) {
    return {
      kind: "error",
      message: error.message,
      details: error.details
    };
  }

  return {
    kind: "error",
    message: error instanceof Error ? error.message : "Request failed."
  };
}
