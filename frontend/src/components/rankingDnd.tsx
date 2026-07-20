import { useDraggable, useDroppable } from "@dnd-kit/core";
import { SortableContext, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { GripVertical } from "lucide-react";
import type { Book } from "../types";

export const AVAILABLE_BOOKS_DROPZONE_ID = "available-books-dropzone";
export const RANKED_BOOKS_DROPZONE_ID = "ranked-books-dropzone";

type BookCardProps = {
  book: Book;
};

type RankedBookCardProps = BookCardProps & {
  rank: number;
};

type BookListProps = {
  books: Book[];
};

function DraggableBookCard({ book }: BookCardProps) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({ id: book.id });
  const style = {
    transform: CSS.Translate.toString(transform)
  };

  return (
    <button
      type="button"
      className={`ranking-card ${isDragging ? "dragging" : ""}`}
      ref={setNodeRef}
      style={style}
      {...listeners}
      {...attributes}
    >
      <GripVertical size={17} />
      <span>
        <strong>{book.title}</strong>
        <small>Capacity {book.capacity}</small>
      </span>
    </button>
  );
}

export function RankingDragOverlay({ book }: BookCardProps) {
  return (
    <div className="ranking-card drag-overlay-card">
      <GripVertical size={17} />
      <span>
        <strong>{book.title}</strong>
        <small>Capacity {book.capacity}</small>
      </span>
    </div>
  );
}

export function AvailableBooksDropZone({ books }: BookListProps) {
  const { setNodeRef, isOver } = useDroppable({ id: AVAILABLE_BOOKS_DROPZONE_ID });

  return (
    <div className={`ranking-column ranking-dropzone ${isOver ? "over" : ""}`} ref={setNodeRef}>
      <div className="ranking-column-heading">
        <h3>Book list</h3>
        <span>{books.length}</span>
      </div>
      <div className="ranking-list">
        {books.length === 0 && <p className="empty-state">All books are ranked.</p>}
        {books.map((book) => (
          <DraggableBookCard book={book} key={book.id} />
        ))}
      </div>
    </div>
  );
}

export function RankingDropZone({ books }: BookListProps) {
  const { setNodeRef, isOver } = useDroppable({ id: RANKED_BOOKS_DROPZONE_ID });

  return (
    <div className={`ranking-column ranking-dropzone ${isOver ? "over" : ""}`} ref={setNodeRef}>
      <div className="ranking-column-heading">
        <h3>Rankings</h3>
      </div>
      <SortableContext items={books.map((book) => book.id)} strategy={verticalListSortingStrategy}>
        <div className="ranking-list ranked">
          {books.length === 0 && <p className="empty-state">Drag books here in order from highest to lowest.</p>}
          {books.map((book, index) => (
            <SortableRankedBookCard book={book} key={book.id} rank={index + 1} />
          ))}
        </div>
      </SortableContext>
    </div>
  );
}

function SortableRankedBookCard({ book, rank }: RankedBookCardProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: book.id });
  const style = {
    transform: CSS.Transform.toString(transform),
    transition
  };

  return (
    <button
      type="button"
      className={`ranking-card ranked ${isDragging ? "dragging" : ""}`}
      ref={setNodeRef}
      style={style}
      {...listeners}
      {...attributes}
      aria-label={`Move ${book.title}`}
    >
      <GripVertical size={17} />
      <span className="ranking-position">{rank}</span>
      <span>
        <strong>{book.title}</strong>
        <small>Capacity {book.capacity}</small>
      </span>
    </button>
  );
}
