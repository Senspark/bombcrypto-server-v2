/**
 * YYYY-MM-DD
 * @param date
 */
function toYearMonthDay(date: Date): string {
    return date.toLocaleDateString('en-CA');
}

function isToday(date: Date): boolean {
    const today = new Date();
    return date.getDate() === today.getDate() && date.getMonth() === today.getMonth() && date.getFullYear() === today.getFullYear();
}

function isFuture(date: Date): boolean {
    const today = new Date();
    return date.getDate() > today.getDate() && date.getMonth() > today.getMonth() && date.getFullYear() > today.getFullYear();
}

function isValid(date: Date): boolean {
    return !isNaN(date.getTime());
}

function today(): string {
    return toYearMonthDay(new Date());
}

function todaySubtractDays(days: number): Date {
    return new Date(new Date().getTime() - days * 24 * 60 * 60 * 1000);
}

/**
 * ISO format: YYYY-MM-DD
 * @param dateStr
 */
function parse(dateStr: string): Date {
    return new Date(dateStr);
}

const DateUtils = {
    toYearMonthDay,
    isToday,
    isFuture,
    isValid,
    today,
    todaySubtractDays,
    parse
};

export default DateUtils;