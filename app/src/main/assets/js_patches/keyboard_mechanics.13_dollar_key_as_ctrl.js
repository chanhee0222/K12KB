// @name 13. $ key (KEYCODE_4) works as Ctrl when held
// Клавиша $ (KEYCODE_4; в русских раскладках на её месте «б») вне режима Alt работает как Ctrl, пока её держат: $+A/C/V/X/Z и т.д. Одна она ничего не печатает. Нажатия делают то же, что Speed-клавиша (кроме SYM+нажатие — голосового ввода): одно — выход из режима свайпа-курсора, Shift+$ — показать/скрыть панель свайпа, двойное — режим прокрутки свайпом, тройное — включить/выключить жесты. В режиме Alt (Alt, Alt+Shift, одно нажатие Alt, SYM-панель) печатает как раньше.

var kgps = json["key-group-processors"];

function hasMeta(action, name) {
    var names = action["meta-mode-method-names"];
    return names != null && names.indexOf(name) >= 0;
}

function copyAction(action) {
    return JSON.parse(JSON.stringify(action));
}

function findGroup(keyCode) {
    for (var i = 0; i < kgps.length; i++) {
        if (kgps[i]["key-codes"].indexOf(keyCode) >= 0)
            return kgps[i];
    }
    return null;
}

// Действия буквенной группы для режима Alt и прозрачность для набора номера — копии, не ссылки
function copyAltAndDigitsPadActions(actions) {
    var result = [];
    for (var i = 0; i < actions.length; i++) {
        var a = actions[i];
        if (hasMeta(a, "MetaIsAltMode")
                || (hasMeta(a, "InputIsDigitsPad") && a["action-method-name"] === "ActionSetKeyTransparency")) {
            result.push(copyAction(a));
        }
    }
    return result;
}

// Группа Speed-клавиши (KEYCODE_CTRL_RIGHT; в той же группе KEYCODE_CTRL_LEFT)
var speedKey = findGroup("KEYCODE_CTRL_LEFT");

// Действия Speed-клавиши по нажатиям — копии, без SYM+нажатие (голосовой ввод)
function copySpeedKeyActions(listName) {
    var actions = speedKey != null ? (speedKey[listName] || []) : [];
    var result = [];
    for (var i = 0; i < actions.length; i++) {
        if (!hasMeta(actions[i], "MetaIsSymPressed"))
            result.push(copyAction(actions[i]));
    }
    return result;
}

// Режим Alt был включён при нажатии: клавиша печатает, Ctrl и жесты не трогаем
function altAtKeyDownStop() {
    return {
        "meta-mode-method-names": ["MetaWasAltModeAtKeyDown"],
        "action-method-name": "ActionDoNothing",
        "stop-processing-at-success-result": true
    };
}

// Набор номера: клавиша прозрачна, ни Ctrl, ни жестов
function digitsPadStop() {
    return {
        "meta-mode-method-names": ["InputIsDigitsPad"],
        "action-method-name": "ActionDoNothing",
        "stop-processing-at-success-result": true
    };
}

// K12KB выполняет сначала действия с условиями (meta-mode-method-names), потом без условий,
// каждую часть по порядку. Условие MetaIsActionBeforeMeta истинно всегда: Ctrl включается
// в первой части, сразу после защит, раньше действий Speed-клавиши с условиями
function enableCtrl() {
    return {
        "meta-mode-method-names": ["MetaIsActionBeforeMeta"],
        "action-method-name": "ActionEnableHoldCtrlMode",
        "method-needs-key-press-parameter": true
    };
}

// Двойное и тройное нажатие: печать в режиме Alt, затем Ctrl, и только потом действия
// Speed-клавиши — у них есть остановка на успехе, и до Ctrl дело бы не дошло
function multiPress(altActions, speedKeyActions) {
    var result = copyAltAndDigitsPadActions(altActions);
    result.push(altAtKeyDownStop());
    result.push(digitsPadStop());
    result.push(enableCtrl());
    return result.concat(speedKeyActions);
}

var letters = findGroup("KEYCODE_4");

if (letters != null) {
    letters["key-codes"].splice(letters["key-codes"].indexOf("KEYCODE_4"), 1);

    // Списков действий в базовом json может не оказаться: тогда считаем их пустыми.
    // Короткое нажатие идёт при нажатии клавиши; Ctrl включает on-hold-on сразу после него.
    var shortPress = copyAltAndDigitsPadActions(letters["on-short-press"] || []);
    shortPress.push(altAtKeyDownStop());
    shortPress.push(digitsPadStop());

    var letterDoublePress = letters["on-double-press"] || [];

    // Без on-long-press: только тогда K12KB ведёт клавишу по пути удержания (Ctrl сразу при нажатии)
    kgps.push({
        "key-codes": ["KEYCODE_4"],
        "on-short-press": shortPress.concat(copySpeedKeyActions("on-short-press")),
        "on-double-press": multiPress(letterDoublePress, copySpeedKeyActions("on-double-press")),
        // С on-triple-press третье быстрое нажатие идёт сюда, а не в короткое или двойное.
        // В режиме Alt печатаем как буквенная группа: тройного у неё нет — как её двойное
        "on-triple-press": multiPress(letters["on-triple-press"] || letterDoublePress,
                copySpeedKeyActions("on-triple-press")),
        "on-hold-on": [
            altAtKeyDownStop(),
            digitsPadStop(),
            enableCtrl()
        ],
        // Отпускание после любого нажатия (одного, двойного, тройного): Ctrl отпускаем
        "on-hold-off": [
            // Набор номера: Ctrl не включали, лишний Ctrl-up не шлём
            digitsPadStop(),
            {
                "action-method-name": "ActionDisableHoldCtrlMode",
                "method-needs-key-press-parameter": true
            }
        ]
    });

    // Буква, зажатая при Ctrl, ничего не делает: защиту ставим первой, раньше удаления символа
    var longPress = letters["on-long-press"] || [];
    for (var j = 0; j < longPress.length; j++) {
        if (hasMeta(longPress[j], "MetaIsCtrlPressed") && longPress[j]["action-method-name"] === "ActionDoNothing") {
            longPress.unshift(longPress.splice(j, 1)[0]);
            break;
        }
    }
}
