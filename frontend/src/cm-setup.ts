import { java } from '@codemirror/lang-java'
import { javascript } from '@codemirror/lang-javascript'
import { python } from '@codemirror/lang-python'
import { php } from '@codemirror/lang-php'
import { StreamLanguage } from '@codemirror/language'
import { go } from '@codemirror/legacy-modes/mode/go'
import { clike } from '@codemirror/legacy-modes/mode/clike'
import type { Language } from '@/types/vuln'
import type { Extension } from '@codemirror/state'

/** Map our Language type to a CodeMirror 6 language extension. */
export function getLanguageExt(lang: Language): Extension {
  switch (lang) {
    case 'java':
      return java()
    case 'go':
      // go from legacy-modes is a StreamParser object; the type def is wrong
      return StreamLanguage.define(go as any)
    case 'python':
      return python()
    case 'typescript':
      return javascript({ typescript: true })
    case 'javascript':
      return javascript()
    case 'php':
      return php()
    case 'csharp':
      // clike from legacy-modes is a StreamParser object; the type def is wrong
      return StreamLanguage.define(clike as any)
  }
}
