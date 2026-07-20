import type { KeyboardEvent } from "react";

export function getPollJoinCode() {
  const path = window.location.pathname;

  if (!path.startsWith("/poll/")) {
    return "";
  }

  return decodeURIComponent(path.slice("/poll/".length).split("/")[0] ?? "")
    .trim()
    .toUpperCase();
}

export function buildPollUrl(joinCode: string | undefined) {
  if (!joinCode) {
    return "-";
  }

  return `${window.location.origin}/poll/${encodeURIComponent(joinCode)}`;
}

export function submitOnEnter(action: () => void | Promise<void>) {
  return (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key !== "Enter") {
      return;
    }

    event.preventDefault();
    void action();
  };
}

export function formatPercent(value: number | undefined) {
  if (value === undefined) {
    return "-";
  }

  return `${Math.round(value * 100)}%`;
}

export function ordinalLabel(value: number) {
  const labels = ["First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth", "Tenth"];

  return labels[value - 1] ?? `${value}th`;
}
