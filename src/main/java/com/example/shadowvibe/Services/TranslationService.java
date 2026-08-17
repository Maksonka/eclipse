package com.example.shadowvibe.Services;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TranslationService {

    private static final Pattern LATIN = Pattern.compile("[a-zA-Z]");
    private static final Pattern CYRILLIC = Pattern.compile("[а-яёА-ЯЁ]");
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

    private static final Map<String, String> PHRASES = new LinkedHashMap<>();
    private static final Map<String, String> EN_RU = new HashMap<>();
    private static final Map<String, String> RU_EN = new HashMap<>();

    private static void seed(String data) {
        for (String line : data.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            int eq = t.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = t.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String val = t.substring(eq + 1).trim();
            if (key.contains(" ")) {
                PHRASES.put(key, val);
            } else {
                EN_RU.put(key, val);
            }
            RU_EN.putIfAbsent(val.toLowerCase(Locale.ROOT), key);
        }
    }

    static {
        seed("""
                hello=привет
                hi=привет
                hey=привет
                bye=пока
                goodbye=до свидания
                yes=да
                yeah=да
                no=нет
                ok=ок
                please=пожалуйста
                sorry=прости
                thanks=спасибо
                thank=спасибо
                friend=друг
                friends=друзья
                bro=брат
                brother=брат
                sister=сестра
                mom=мама
                dad=папа
                family=семья
                home=дом
                house=дом
                work=работа
                job=работа
                office=офис
                school=школа
                university=университет
                study=учиться
                learn=учить
                read=читать
                book=книга
                write=писать
                letter=письмо
                message=сообщение
                messages=сообщения
                chat=чат
                call=звонок
                phone=телефон
                computer=компьютер
                laptop=ноутбук
                internet=интернет
                online=онлайн
                file=файл
                files=файлы
                photo=фото
                photos=фото
                picture=картинка
                video=видео
                music=музыка
                song=песня
                voice=голос
                audio=аудио
                meeting=встреча
                date=дата
                time=время
                hour=час
                minute=минута
                day=день
                week=неделя
                month=месяц
                year=год
                today=сегодня
                tomorrow=завтра
                yesterday=вчера
                now=сейчас
                soon=скоро
                later=позже
                morning=утро
                evening=вечер
                night=ночь
                weekend=выходные
                weather=погода
                sun=солнце
                rain=дождь
                snow=снег
                hot=жарко
                cold=холодно
                nice=хороший
                good=хорошо
                great=отлично
                awesome=классно
                cool=круто
                fine=нормально
                bad=плохо
                beautiful=красиво
                pretty=симпатично
                interesting=интересно
                boring=скучно
                fun=весело
                funny=смешно
                happy=счастлив
                sad=грустный
                angry=злой
                tired=устал
                busy=занят
                hungry=голоден
                big=большой
                small=маленький
                long=длинный
                short=короткий
                new=новый
                old=старый
                young=молодой
                fast=быстро
                slow=медленно
                easy=легко
                hard=сложно
                simple=просто
                cheap=дешёвый
                expensive=дорогой
                free=бесплатно
                real=настоящий
                true=правда
                right=правильно
                wrong=неправильно
                first=первый
                last=последний
                next=следующий
                other=другой
                same=такой же
                only=только
                always=всегда
                never=никогда
                sometimes=иногда
                often=часто
                really=реально
                very=очень
                too=тоже
                also=также
                maybe=может быть
                probably=наверное
                together=вместе
                again=снова
                about=о
                because=потому что
                before=до
                after=после
                between=между
                without=без
                with=с
                from=из
                over=над
                under=под
                near=рядом
                far=далеко
                here=здесь
                there=там
                where=где
                when=когда
                why=почему
                what=что
                which=который
                who=кто
                how=как
                i=я
                you=ты
                he=он
                she=она
                it=это
                we=мы
                they=они
                me=мне
                him=ему
                her=её
                us=нас
                them=их
                my=мой
                your=твой
                his=его
                their=их
                our=наш
                this=это
                that=то
                these=эти
                those=те
                will=буду
                would=бы
                could=мог
                should=следует
                must=должен
                can=могу
                is=есть
                are=есть
                was=был
                were=были
                be=быть
                have=иметь
                has=имеет
                had=имел
                do=делать
                does=делает
                did=делал
                go=идти
                went=пошёл
                come=прийти
                came=пришёл
                get=получить
                got=получил
                make=делать
                made=сделал
                see=видеть
                saw=видел
                look=смотреть
                watch=смотреть
                hear=слышать
                listen=слушать
                say=сказать
                said=сказал
                tell=рассказать
                talk=говорить
                speak=говорить
                ask=спросить
                answer=ответить
                know=знать
                think=думать
                believe=верить
                hope=надеяться
                want=хотеть
                need=нужно
                like=нравится
                love=люблю
                hate=ненавидеть
                eat=есть
                drink=пить
                sleep=спать
                walk=гулять
                run=бежать
                drive=водить
                travel=путешествовать
                fly=лететь
                wait=ждать
                stay=остаться
                live=жить
                start=начать
                finish=закончить
                stop=стоп
                help=помощь
                open=открыть
                close=закрыть
                buy=купить
                sell=продать
                pay=платить
                money=деньги
                price=цена
                shop=магазин
                restaurant=ресторан
                cafe=кафе
                food=еда
                water=вода
                coffee=кофе
                tea=чай
                beer=пиво
                wine=вино
                bread=хлеб
                meat=мясо
                fish=рыба
                cheese=сыр
                milk=молоко
                breakfast=завтрак
                lunch=обед
                dinner=ужин
                party=вечеринка
                concert=концерт
                movie=фильм
                movies=фильмы
                film=фильм
                game=игра
                games=игры
                play=играть
                football=футбол
                sport=спорт
                team=команда
                win=победа
                lose=проиграть
                trip=поездка
                vacation=отпуск
                holiday=праздник
                city=город
                street=улица
                road=дорога
                car=машина
                bus=автобус
                train=поезд
                plane=самолёт
                taxi=такси
                airport=аэропорт
                hotel=отель
                room=комната
                key=ключ
                door=дверь
                window=окно
                table=стол
                bed=кровать
                clothes=одежда
                shirt=рубашка
                shoes=обувь
                bag=сумка
                gift=подарок
                flowers=цветы
                dog=собака
                cat=кот
                bird=птица
                health=здоровье
                doctor=врач
                hospital=больница
                medicine=лекарство
                problem=проблема
                question=вопрос
                idea=идея
                plan=план
                story=история
                news=новости
                name=имя
                number=номер
                people=люди
                person=человек
                man=мужчина
                woman=женщина
                boy=мальчик
                girl=девочка
                kid=ребёнок
                kids=дети
                guy=парень
                parents=родители
                wife=жена
                husband=муж
                life=жизнь
                world=мир
                place=место
                things=вещи
                everything=всё
                something=что-то
                nothing=ничего
                everyone=все
                all=все
                some=немного
                any=любой
                many=много
                much=много
                more=больше
                most=большинство
                less=меньше
                little=мало
                enough=достаточно
                up=вверх
                down=вниз
                out=вне
                in=в
                on=на
                at=в
                for=для
                to=в
                of=из
                by=по
                as=как
                or=или
                and=и
                but=но
                if=если
                so=так
                well=ну
                how are you=как дела
                what's up=как дела
                good morning=доброе утро
                good evening=добрый вечер
                good night=доброй ночи
                see you=увидимся
                see you later=увидимся позже
                see you soon=скоро увидимся
                see you tomorrow=до завтра
                thank you=спасибо
                you are welcome=пожалуйста
                excuse me=извините
                i am sorry=прости
                i love you=я тебя люблю
                i don't know=я не знаю
                i don't understand=я не понимаю
                i don't want=я не хочу
                i don't like=мне не нравится
                i don't care=мне всё равно
                no problem=без проблем
                of course=конечно
                right now=прямо сейчас
                come on=давай
                let's go=пойдём
                take care=береги себя
                good luck=удачи
                happy birthday=с днём рождения
                i'm fine=я в порядке
                i'm tired=я устал
                i'm busy=я занят
                i'm home=я дома
                i'm on my way=я в пути
                i'm coming=я иду
                i have to go=мне пора
                i'll call you=я тебе позвоню
                i'll text you=я тебе напишу
                i'll be right back=я сейчас вернусь
                i'll be there soon=я скоро буду
                i'll wait for you=я тебя подожду
                i'll help you=я тебе помогу
                call me later=позвони мне позже
                wait for me=подожди меня
                come here=иди сюда
                come back=вернись
                go home=иди домой
                go to bed=иди спать
                wake up=просыпайся
                get up=вставай
                sit down=садись
                hurry up=поторопись
                calm down=успокойся
                look for=искать
                looking for=ищет
                looks like=похоже
                let me know=дай мне знать
                let's talk=давай поговорим
                let's meet=давай встретимся
                let's eat=давай поедим
                do you know=ты знаешь
                do you understand=ты понимаешь
                do you have=у тебя есть
                do you want=ты хочешь
                do you like=тебе нравится
                are you ready=ты готов
                are you ok=ты в порядке
                are you sure=ты уверен
                are you coming=ты идёшь
                i'm sure=я уверен
                i'm not sure=я не уверен
                i agree=я согласен
                i hope so=я надеюсь
                i mean=я имею в виду
                i see=понятно
                i understand=я понимаю
                i got it=понял
                got it=понял
                no idea=понятия не имею
                by the way=кстати
                in fact=на самом деле
                hold on=подожди
                just a moment=момент
                one more=ещё один
                what is this=что это
                what is it=что это
                what is wrong=что не так
                what is going on=что происходит
                what is happening=что происходит
                what happened=что случилось
                what's your name=как тебя зовут
                my name is=меня зовут
                what's new=что нового
                what are you doing=что делаешь
                what are you talking about=о чём ты
                what do you think=что думаешь
                what do you want=что ты хочешь
                what did you say=что ты сказал
                what can i do=что я могу сделать
                what should i do=что мне делать
                what time is it=который час
                how much=сколько
                how long=как долго
                how many=сколько
                how are you doing=как дела
                how's it going=как дела
                how was your day=как прошёл день
                where are you=где ты
                where are you going=куда ты идёшь
                where do you live=где ты живёшь
                why not=почему бы и нет
                what about=как насчёт
                nice to meet you=приятно познакомиться
                nice to see you=рад тебя видеть
                long time no see=давно не виделись
                have fun=развлекайся
                have a nice day=хорошего дня
                have a nice weekend=хороших выходных
                good job=отличная работа
                well done=молодец
                congratulations=поздравляю
                happy new year=с новым годом
                merry christmas=с рождеством
                of course not=конечно нет
                no way=не может быть
                not yet=ещё нет
                not really=не совсем
                definitely=определённо
                absolutely=абсолютно
                exactly=именно
                all right=хорошо
                understood=понятно
                this evening=сегодня вечером
                this weekend=в эти выходные
                last night=прошлой ночью
                after work=после работы
                a lot=много
                a little=немного
                a few=несколько
                too much=слишком много
                call me=позвони мне
                text me=напиши мне
                wait a minute=подожди минуту
                can you help me=можешь мне помочь
                can you hear me=ты меня слышишь
                can you repeat=можешь повторить
                do you speak english=ты говоришь по-английски
                how do you say=как сказать
                it means=это значит
                i will be there=я буду там
                i will come=я приду
                i will go=я пойду
                i will tell you=я тебе скажу
                i will show you=я тебе покажу
                i will give you=я тебе дам
                let me help you=дай мне помочь тебе
                let me think=дай мне подумать
                let me see=дай мне посмотреть
                let me try=дай мне попробовать
                let me go=отпусти меня
                let's do it=давай сделаем это
                let's try=давай попробуем
                let's not=давай не будем
                let's get started=давай начнём
                let's meet tomorrow=давай встретимся завтра
                see you at=увидимся в
                meet me at=встретимся в
                pick me up=забери меня
                take me to=отвези меня в
                i need to go=мне нужно идти
                i need your help=мне нужна твоя помощь
                i want to go=я хочу пойти
                i want to see=я хочу посмотреть
                i want to talk=я хочу поговорить
                i want to ask=я хочу спросить
                i want to know=я хочу знать
                i want to eat=я хочу есть
                i want to sleep=я хочу спать
                i want to go home=я хочу домой
                i want to go to the store=я хочу пойти в магазин
                i want to go to the cinema=я хочу пойти в кино
                i want to go to the park=я хочу пойти в парк
                i want to go to the gym=я хочу пойти в зал
                i want to go to the beach=я хочу пойти на пляж
                i want to go to the party=я хочу пойти на вечеринку
                i want to go to the concert=я хочу пойти на концерт
                i want to go to the restaurant=я хочу пойти в ресторан
                i want to go to the movies=я хочу пойти в кино
                i want to go to the doctor=я хочу пойти к врачу
                i want to go to the bank=я хочу пойти в банк
                i want to go to the office=я хочу пойти в офис
                i want to go to the university=я хочу пойти в университет
                i want to go to the hotel=я хочу пойти в отель
                """);
    }

    public String translateAuto(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        boolean hasLatin = LATIN.matcher(text).find();
        boolean hasCyrillic = CYRILLIC.matcher(text).find();
        if (hasLatin && !hasCyrillic) {
            return translateTo(text, EN_RU);
        }
        if (hasCyrillic && !hasLatin) {
            return translateTo(text, RU_EN);
        }
        return text;
    }

    private String translateTo(String text, Map<String, String> dict) {
        String lower = text.toLowerCase(Locale.ROOT);
        String worked = lower;
        for (Map.Entry<String, String> phrase : PHRASES.entrySet()) {
            worked = replacePhrase(worked, phrase.getKey(), phrase.getValue());
        }

        Matcher m = WORD.matcher(worked);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            sb.append(worked, last, m.start());
            String word = m.group();
            String lowerWord = word.toLowerCase(Locale.ROOT);
            String trans = dict.get(lowerWord);
            if (trans == null) {
                sb.append(word);
            } else {
                sb.append(applyCase(trans, word));
            }
            last = m.end();
        }
        sb.append(worked, last, worked.length());

        String result = sb.toString();
        if (result.equals(lower)) {
            return text;
        }
        return capitalizeFirst(result);
    }

    private String replacePhrase(String text, String phrase, String trans) {
        return text.replace(phrase, trans);
    }

    private String applyCase(String trans, String original) {
        if (original.length() > 0 && Character.isUpperCase(original.charAt(0))) {
            if (original.equals(original.toUpperCase(Locale.ROOT)) && original.length() > 1) {
                return trans.toUpperCase(Locale.ROOT);
            }
            return Character.toUpperCase(trans.charAt(0)) + trans.substring(1);
        }
        return trans;
    }

    private String capitalizeFirst(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}