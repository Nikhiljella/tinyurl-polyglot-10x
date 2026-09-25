import crypto from "crypto";

export interface UrlRecord {
  id: string;
  original_url: string;
  short_url: string;
  click_count: number;
  created_at: string;
}

const BASE62_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

export function generateSlug(length = 7): string {
  let result = "";
  const randomBytes = crypto.randomBytes(length);
  for (let i = 0; i < length; i++) {
    result += BASE62_CHARS[randomBytes[i] % BASE62_CHARS.length];
  }
  return result;
}

export class MemoryStore {
  private urls = new Map<string, UrlRecord>();

  save(record: UrlRecord): boolean {
    if (this.urls.has(record.id)) {
      return false;
    }
    this.urls.set(record.id, record);
    return true;
  }

  get(id: string): UrlRecord | undefined {
    return this.urls.get(id);
  }

  incrementClick(id: string): UrlRecord | undefined {
    const record = this.urls.get(id);
    if (!record) return undefined;
    record.click_count += 1;
    return record;
  }

  has(id: string): boolean {
    return this.urls.has(id);
  }
}
