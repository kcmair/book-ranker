import type { ReactNode } from "react";
import { AlertTriangle, ArrowUpDown, Check, CheckCircle2, Copy, Loader2, X } from "lucide-react";
import type { Confirmation, Notice } from "../utils/appTypes";

type PanelProps = {
  title: string;
  icon: ReactNode;
  children: ReactNode;
  wide?: boolean;
};

type ActionButtonProps = {
  icon: ReactNode;
  label: string;
  busy: boolean;
  onClick: () => void;
  variant?: "primary" | "secondary";
};

type MetricProps = {
  label: string;
  value: string | number;
  valueTone?: "danger";
};

type SortButtonProps = {
  label: string;
  active: boolean;
  onClick: () => void;
};

type CopyableMetricProps = {
  label: string;
  value: string | number;
  copied: boolean;
  onCopy: () => void;
};

type NoticeModalProps = {
  notice: Notice;
  onDismiss: () => void;
};

type ConfirmationDialogProps = {
  confirmation: Confirmation;
  onCancel: () => void;
};

export function Panel({ title, icon, children, wide }: PanelProps) {
  return (
    <article className={`panel ${wide ? "wide" : ""}`}>
      <div className="panel-heading">
        {icon}
        <h2>{title}</h2>
      </div>
      {children}
    </article>
  );
}

export function ActionButton({ icon, label, busy, onClick, variant = "primary" }: ActionButtonProps) {
  return (
    <button className={`action-button ${variant}`} disabled={busy} onClick={onClick}>
      {busy ? <Loader2 className="spin" size={16} /> : icon}
      {label}
    </button>
  );
}

export function Metric({ label, value, valueTone }: MetricProps) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong className={valueTone === "danger" ? "danger-text" : undefined}>{value}</strong>
    </div>
  );
}

export function SortButton({ label, active, onClick }: SortButtonProps) {
  return (
    <button type="button" className={`sort-button ${active ? "active" : ""}`} onClick={onClick}>
      <span>{label}</span>
      <ArrowUpDown size={14} />
    </button>
  );
}

export function CopyableMetric({ label, value, copied, onCopy }: CopyableMetricProps) {
  return (
    <div className="metric copyable-metric">
      <button type="button" aria-label={`Copy ${label}`} onClick={onCopy}>
        {copied ? <Check size={15} /> : <Copy size={15} />}
      </button>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

export function NoticeModal({ notice, onDismiss }: NoticeModalProps) {
  if (!notice) {
    return null;
  }

  const title = notice.kind === "success" ? "Success" : "Something went wrong";
  const icon = notice.kind === "success" ? <CheckCircle2 size={22} /> : <AlertTriangle size={22} />;

  return (
    <div className="modal-backdrop" role="presentation">
      <div
        className={`notice-dialog ${notice.kind}`}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="notice-dialog-title"
      >
        <button type="button" className="icon-button notice-close" aria-label="Dismiss message" onClick={onDismiss}>
          <X size={17} />
        </button>
        <div className="notice-heading">
          {icon}
          <h2 id="notice-dialog-title">{title}</h2>
        </div>
        <p>{notice.message}</p>
        {notice.details && notice.details.length > 0 && (
          <ul className="notice-details">
            {notice.details.map((detail, index) => (
              <li key={`${detail}-${index}`}>{detail}</li>
            ))}
          </ul>
        )}
        <div className="confirm-actions">
          <button type="button" onClick={onDismiss}>
            Dismiss
          </button>
        </div>
      </div>
    </div>
  );
}

export function ConfirmationDialog({ confirmation, onCancel }: ConfirmationDialogProps) {
  if (!confirmation) {
    return null;
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <div className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title">
        <div>
          <h2 id="confirm-dialog-title">{confirmation.title}</h2>
          <p>{confirmation.message}</p>
        </div>
        <div className="confirm-actions">
          <button type="button" onClick={onCancel}>
            Cancel
          </button>
          <button type="button" className={confirmation.confirmTone ?? "danger"} onClick={confirmation.onConfirm}>
            {confirmation.confirmLabel ?? "Confirm"}
          </button>
        </div>
      </div>
    </div>
  );
}
