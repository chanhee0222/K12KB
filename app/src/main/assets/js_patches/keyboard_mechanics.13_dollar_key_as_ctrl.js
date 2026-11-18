// @name 13. $ key (KEYCODE_4) works as Ctrl when held
// Клавиша $ (KEYCODE_4; в русских раскладках на её месте «б») вне режима Alt работает как Ctrl, пока её держат: $+A/C/V/X/Z и т.д. Одна она ничего не печатает. В режиме Alt (Alt, Alt+Shift, одно нажатие Alt, SYM-панель) печатает как раньше.

var kgps = json["key-group-processors"];

function hasMeta(action, name) {
    var names = action["meta-mode-method-names"];
    return names != null && names.indexOf(name) >= 0;
}

function copyAction(action) {
    return JSON.parse(JSON.stringify(action));
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

// Режим Alt был включён при нажатии: клавиша печатает, Ctrl не включаем
function altAtKeyDownStop() {
    return {
        "meta-mode-method-names": ["MetaWasAltModeAtKeyDown"],
        "action-method-name": "ActionDoNothing",
        "stop-processing-at-success-result": true
    };
}

function enableCtrl() {
    return {
        "action-method-name": "ActionEnableHoldCtrlMode",
        "method-needs-key-press-parameter": true
    };
}

var letters = null;
for (var i = 0; i < kgps.length; i++) {
    if (kgps[i]["key-codes"].indexOf("KEYCODE_4") >= 0) {
        letters = kgps[i];
        break;
    }
}

if (letters != null) {
    letters["key-codes"].splice(letters["key-codes"].indexOf("KEYCODE_4"), 1);

    // Списков действий в базовом json может не оказаться: тогда считаем их пустыми
    var doublePress = copyAltAndDigitsPadActions(letters["on-double-press"] || []);
    doublePress.push(altAtKeyDownStop());
    doublePress.push(enableCtrl());

    // Без on-long-press: только тогда K12KB ведёт клавишу по пути удержания (Ctrl сразу при нажатии)
    kgps.push({
        "key-codes": ["KEYCODE_4"],
        "on-short-press": copyAltAndDigitsPadActions(letters["on-short-press"] || []),
        "on-double-press": doublePress,
        "on-hold-on": [
            altAtKeyDownStop(),
            {
                "meta-mode-method-names": ["InputIsDigitsPad"],
                "action-method-name": "ActionDoNothing",
                "stop-processing-at-success-result": true
            },
            enableCtrl()
        ],
        "on-hold-off": [
            // Набор номера: Ctrl не включали, лишний Ctrl-up не шлём
            {
                "meta-mode-method-names": ["InputIsDigitsPad"],
                "action-method-name": "ActionDoNothing",
                "stop-processing-at-success-result": true
            },
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
