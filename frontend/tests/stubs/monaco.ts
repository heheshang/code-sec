// Stub of the monaco-editor API used by CodeViewer and FixSnippetEditor.
// Only the symbols referenced at module-load time are needed; the
// real editor wrapper is mocked separately in each test file.

export class Range {
  public startLineNumber: number
  public startColumn: number
  public endLineNumber: number
  public endColumn: number
  constructor(sl: number, sc: number, el: number, ec: number) {
    this.startLineNumber = sl
    this.startColumn = sc
    this.endLineNumber = el
    this.endColumn = ec
  }
}

export const editor = {
  setModelLanguage: (): void => undefined,
}
