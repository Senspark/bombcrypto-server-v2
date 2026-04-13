import dayjs from "dayjs";
import utc from "dayjs/plugin/utc"; // Import UTC plugin

dayjs.extend(utc); // Extend dayjs with UTC plugin

/**
 * YYYY-MM-DD
 * @param date
 */
function toYearMonthDay(date: Date): string {
    return date.toISOString().split('T')[0];
}

function toStandardFormat(time: Date): string {
    return dayjs(time).format('YYYY-MM-DD HH:mm:ss');
}

function isToday(date: Date): boolean {
    const today = new Date();
    return date.getDate() === today.getDate() && date.getMonth() === today.getMonth() && date.getFullYear() === today.getFullYear();
}

function isFuture(date: Date): boolean {
    return date > new Date();
}

function isValid(date: Date): boolean {
    return !isNaN(date.getTime());
}

function today(): string {
    return toYearMonthDay(new Date());
}

function todaySubtractDays(days: number): string {
    return toYearMonthDay(new Date(new Date().getTime() - days * 24 * 60 * 60 * 1000));
}

/**
 * ISO format: YYYY-MM-DD
 */
function parse(dateStr: string): Date {
    return new Date(dateStr);
}

const DAY_FORMAT = 'YYYY-MM-DD';

const DateUtils = {
    DATE_FORMAT: DAY_FORMAT,
    toYearMonthDay,
    toStandardFormat,
    isToday,
    isFuture,
    isValid,
    today,
    todaySubtractDays,
    parse,
};

export default DateUtils;
