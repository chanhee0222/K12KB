# Third-Party Notices

K12KB includes work derived from the projects below. Their license notices are
retained here.

## ReteKey (Hangul composition automaton)

- Project: ReteKey (https://github.com/rubidus-api/retekey_apk)
- Revision: 0e701a4480589a66255f6ab1e3be24eafb2d4f27
- License: MIT
- Files (under `app/src/main/java/com/ai10/k12kb/hangul/`): `HangulComposer.java`,
  `HangulTables.java`, `SemanticJamo.java`; tests `HangulComposerTest.java` and
  `HangulTablesTest.java` (under `app/src/test/java/com/ai10/k12kb/hangul/`).
- Changes: package renamed to `com.ai10.k12kb.hangul`; `HangulComposer` gained
  `snapshot()` and `restore()`. Everything else is unchanged.

```
MIT License

Copyright (c) 2026 rubidus-api

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Jamotong

ReteKey's Hangul 2-beolsik composition automaton (`HangulComposer` and
`HangulTables`) is a pure-Java port of the state machine and jamo tables in the
Jamotong project's `src/fsm.c`, `src/fsm.h`, and the 2-beolsik/Unicode portions
of `src/layout.c`. The transition design and the jamo/combination tables are
derived from Jamotong; the output model, reversible compound backspace, and
no-loss behavior are ReteKey's own.

- Project: Jamotong (https://github.com/rubidus-api/jamotong_ime)
- Derived from revision: 90d6eb5ea60d54d320e42656da8b703432bb6d9f
- License: MIT

```
MIT License

Copyright (c) 2026 rubidus-api

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
