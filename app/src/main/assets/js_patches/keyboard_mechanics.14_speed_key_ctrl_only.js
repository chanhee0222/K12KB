// @name 14. Speed key (Ctrl right): only Ctrl when held, no tap actions
// Speed-клавиша (KEYCODE_CTRL_RIGHT) работает только как Ctrl, пока её держат: Speed+буква = Ctrl+буква. Нажатия больше ничего не делают: ни выхода из режима свайпа-курсора, ни панели свайпа (Shift+нажатие), ни прокрутки свайпом (двойное), ни включения жестов (тройное), ни голосового ввода (SYM+нажатие). Сама Speed-клавиша и клавиши, нажатые при ней, не выключают ни прокрутку свайпом, ни свайп-курсор. Остальные клавиши её группы (Ctrl слева, правый Shift, Function, 289) работают как раньше. Действия по нажатиям может взять на себя клавиша $ (патч 13).

var SPEED_KEY = "KEYCODE_CTRL_RIGHT";
// Истинно, если клавишу нажали без системного Ctrl. Speed-клавиша приходит с Ctrl, и любая
// клавиша, нажатая при ней, тоже
var NO_CTRL = "MetaWasNoCtrlMetaAtKeyDown";

var kgps = json["key-group-processors"];

function keyPressAction(name) {
    return {
        "action-method-name": name,
        "method-needs-key-press-parameter": true
    };
}

// Добавляет к условиям действия «без системного Ctrl». MetaIsActionBeforeMeta (истинно всегда)
// заменяем: место действия в очереди то же. Действие без условий получает условие, только если
// allowUnconditioned: K12KB выполняет сначала действия с условиями, и оно сдвинется вперёд
function addNoCtrl(action, allowUnconditioned) {
    var names = action["meta-mode-method-names"];
    if (names == null || names.length == 0) {
        if (allowUnconditioned)
            action["meta-mode-method-names"] = [NO_CTRL];
        return;
    }
    if (names.indexOf(NO_CTRL) >= 0)
        return;
    var pos = names.indexOf("MetaIsActionBeforeMeta");
    if (pos >= 0)
        names[pos] = NO_CTRL;
    else
        names.push(NO_CTRL);
}

function addNoCtrlTo(list, actionName, allowUnconditioned) {
    if (list == null)
        return;
    for (var i = 0; i < list.length; i++) {
        if (list[i] != null && list[i]["action-method-name"] === actionName)
            addNoCtrl(list[i], allowUnconditioned);
    }
}

// Speed-клавишу убираем из всех групп. Общую группу (KEYCODE_CTRL_LEFT и др.) не трогаем:
// патч 13 копирует из неё действия по нажатиям, порядок патчей не важен
var speedKeyFound = false;
if (kgps != null) {
    for (var i = kgps.length - 1; i >= 0; i--) {
        var codes = kgps[i] != null ? kgps[i]["key-codes"] : null;
        if (codes == null)
            continue;
        var pos = codes.indexOf(SPEED_KEY);
        if (pos < 0)
            continue;
        speedKeyFound = true;
        codes.splice(pos, 1);
        // Группу с пустым key-codes K12KB не загрузит
        if (codes.length == 0)
            kgps.splice(i, 1);
    }
}

if (speedKeyFound) {
    // Без on-long-press, но с on-short-press и on-double-press: только так K12KB ведёт клавишу
    // по пути удержания, и Ctrl включается (on-hold-on) сразу при нажатии.
    // Второе и третье быстрое нажатие идут в on-double-press и on-triple-press, а не в
    // on-hold-on, поэтому Ctrl включают и они; отпускает его on-hold-off после любого нажатия
    kgps.push({
        "key-codes": [SPEED_KEY],
        "on-short-press": [
            { "action-method-name": "ActionDoNothing" }
        ],
        "on-double-press": [keyPressAction("ActionEnableHoldCtrlMode")],
        "on-triple-press": [keyPressAction("ActionEnableHoldCtrlMode")],
        "on-hold-on": [keyPressAction("ActionEnableHoldCtrlMode")],
        "on-hold-off": [keyPressAction("ActionDisableHoldCtrlMode")]
    });

    // Короткое нажатие любой клавиши выключает прокрутку свайпом (on-any-key-before-actions).
    // При системном Ctrl — нет: ни нажатие самой Speed-клавиши, ни клавиши при ней
    var anyKey = json["on-any-key-before-actions"];
    if (anyKey != null)
        addNoCtrlTo(anyKey["on-short-press"], "ActionTryDisableGestureInputScrollMode", true);

    // Буквы и другие клавиши первым делом выключают свайп-курсор; при системном Ctrl — нет
    if (kgps != null) {
        for (var g = 0; g < kgps.length; g++) {
            if (kgps[g] != null)
                addNoCtrlTo(kgps[g]["on-short-press"], "ActionTryTurnOffGesturesMode", false);
        }
    }
}
