import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;

public class randomTest {
    public static void main(String[] args) {
        String res = String.format("This is a test %s", "test");
        System.out.println(res);

        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.MONTH,1);
        int january = Calendar.JANUARY;
        int september = Calendar.SEPTEMBER;
        int day =  Calendar.SUNDAY;

        System.out.printf("week of year: %d\n", randomTest.caculate("2021-11-01",Calendar.FRIDAY,1).getWeek());
    }


    /**
     * 计算week and year
     * @param dateStr
     * @param firstOfWeek
     * @param minimalDaysInFirstWeek
     * @return
     */
    static YearAndWeek caculate(String dateStr,int firstOfWeek,int minimalDaysInFirstWeek){
        if(firstOfWeek < Calendar.SUNDAY || firstOfWeek > Calendar.SATURDAY){
            firstOfWeek = Calendar.MONDAY;
        }
        YearAndWeek yearAndWeek = new YearAndWeek();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = simpleDateFormat.parse(dateStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setFirstDayOfWeek(firstOfWeek);
            if(minimalDaysInFirstWeek >= 1 && minimalDaysInFirstWeek <= 7){
                calendar.setMinimalDaysInFirstWeek(minimalDaysInFirstWeek);
            }
            calendar.setTime(date);
            int week = calendar.get(Calendar.WEEK_OF_YEAR);
            int month = calendar.get(Calendar.MONTH);
            int year = calendar.get(Calendar.YEAR);

            if(month == Calendar.JANUARY && week >= 52){
                year = year - 1;
            }

            if(month == Calendar.SEPTEMBER && week == 1){
                year = year + 1;
            }

            yearAndWeek.setWeek(week);
            yearAndWeek.setYear(year);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return yearAndWeek;
    }


    static class YearAndWeek{
        private int year;
        private int week;

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getWeek() {
            return week;
        }

        public String getWeekStr(){
            return week < 10 ? "0"+week : week+"";
        }

        public void setWeek(int week) {
            this.week = week;
        }
    }
}
