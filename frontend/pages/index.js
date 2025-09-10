import { useEffect, useState, useRef } from "react";
import Image from "next/image";
import SearchBox from "../components/SearchBox";

const QUESTIONS = [
  "Что ты чувствуешь, когда смотришь на это изображение?",
  "Какое настроение передаёт эта картинка?",
  "Если бы это было место, хотел(а) бы ты там оказаться?",
  "Какая музыка подошла бы к этому изображению?",
  "Какие три слова описывают эту картину лучше всего?",
  "Какие эмоции вызывает у тебя этот сюжет?",
  "Представь, что это кадр из фильма — о чём этот фильм?",
  "Если бы это был сон, что бы он значил?",
  "Какая цитата подошла бы к этой картинке?",
  "Если бы здесь был герой, кто бы это был?",
  "Какая книга ассоциируется у тебя с этим изображением?",
  "Если бы это было начало истории, какой была бы её концовка?",
  "Что тебя вдохновляет в этой картине?",
  "Какие цвета здесь задают настроение?",
  "Если бы это была обложка альбома, какой жанр музыки внутри?",
  "Куда ведёт эта дорога?",
  "Какую тайну скрывает этот пейзаж?",
  "Какие три прилагательных описывают настроение этого кадра?",
  "Что здесь самое главное?",
  "Какая эмоция у тебя возникает первой?",
  "Если бы это был математический объект, то какой?",
  "Какая формула описывает эту гармонию?",
  "Может ли это изображение символизировать бесконечность?",
  "Какая физическая теория ближе всего к этому сюжету?",
  "Представь, что это иллюстрация к научной статье. О чём она?",
  "Какие законы симметрии ты здесь видишь?",
  "Что напоминает эта структура в терминах геометрии?",
  "Какая часть квантовой физики связана с этим образом?",
  "Может ли это быть моделью многомерного пространства?",
  "Какую задачу по физике можно составить по этой картине?",
  "Если это граф, то какие у него вершины и рёбра?",
  "Что здесь похоже на фрактал?",
  "Какую теорему можно проиллюстрировать этим изображением?",
  "Может ли это быть топологической поверхностью?",
  "Какая формула Лагранжа подошла бы к этому движению?",
  "Видишь ли ты здесь золотое сечение?",
  "Что напоминает тебе этот узор из мира математики?",
  "Какие физические процессы могли бы породить такую картину?",
  "Какое уравнение лучше всего описывает эту динамику?",
  "Какая гипотеза о Вселенной отражена в этом изображении?",
  "Если бы это была картина будущего, каким оно выглядит?",
  "Какие технологии могли бы создать такой мир?",
  "Как это изображение связано с искусственным интеллектом?",
  "Какие опасности можно увидеть в этой картине?",
  "Если бы это был символ эпохи, то какой?",
  "Какие изменения климата можно здесь вообразить?",
  "Какие философские идеи рождаются при виде этого изображения?",
  "Какие чувства вызывает у тебя игра света и тени?",
  "Какую мелодию ты слышишь, глядя сюда?",
  "Что здесь похоже на детскую мечту?",
  "Какая сказка могла бы происходить в этом месте?",
  "Что напоминает это изображение из твоего детства?",
  "Если бы это был персонаж, какие у него черты характера?",
  "Какие мифы можно придумать на основе этой картинки?",
  "Что здесь кажется тебе реальным, а что — фантастическим?",
  "Если бы это было животное, каким бы оно было?",
  "Какая планета могла бы так выглядеть?",
  "Что здесь говорит о бесконечности времени?",
  "Какая парадоксальная идея скрыта в этом сюжете?",
  "Что здесь можно описать уравнением Эйнштейна?",
  "Если бы это был эксперимент, что бы он доказывал?",
  "Какая квантовая частица могла бы здесь существовать?",
  "Какая формула лучше всего описывает гармонию цветов?",
  "Что здесь может быть моделью нейросети?",
  "Если бы это было математическое доказательство, то к чему оно ведёт?",
  "Какие три закона Ньютона проявляются здесь?",
  "Какая энергия заключена в этом изображении?",
  "Какие космические явления напоминают тебе эти формы?",
  "Что здесь похоже на черную дыру?",
  "Какая идея о времени отражена в этом изображении?",
  "Может ли это быть метафорой хаоса?",
  "Какие уравнения хаотической динамики здесь можно представить?",
  "Что напоминает это изображение из области синергетики?",
  "Какую задачу по алгебре можно составить по этой картине?",
  "Какая аксиома могла бы описывать это пространство?",
  "Что здесь похоже на клетку живого организма?",
  "Какая физическая константа ассоциируется с этим изображением?",
  "Какие философские вопросы вызывает у тебя это изображение?",
  "Какую гипотезу о сознании можно здесь вообразить?",
  "Что здесь похоже на древний символ?",
  "Какая идея из квантовой механики отражена в этой картине?",
  "Если бы это был алгоритм, то какой?",
  "Какая мысль первая приходит в голову при виде этой картины?",
  "Какие формы напоминают тебе космос?",
  "Какая математическая модель описывает это равновесие?",
  "Что здесь напоминает тебе теорию струн?",
  "Какая идея из биологии отражена в этой картине?",
  "Если бы это был робот, каким он был бы?",
  "Какие эмоции вызывает у тебя сочетание форм?",
  "Какая абстрактная идея рождается при взгляде сюда?",
  "Какая энергия чувствуется в этом изображении?",
  "Что здесь кажется простым, а что сложным?",
  "Какая картина из истории искусства ближе всего к этой?",
  "Какие стили живописи перекликаются с этим сюжетом?",
  "Если бы это была загадка, каков её ответ?",
  "Что здесь похоже на формулу счастья?",
  "Какие тайны природы напоминает тебе эта картина?",
  "Какая метафора лучше всего описывает это изображение?",
  "Если бы это был новый мир, как бы ты его назвал?",
  "Какая картина известного художника ближе всего по стилю к этому изображению?",
  "Какой художественный стиль можно здесь угадать?",
  "Если бы это было граффити, в каком городе оно появилось бы?",
  "Какую скульптуру можно создать по мотивам этой картинки?",
  "Какая эпоха в истории искусства вдохновила бы такую работу?",
  "Если бы это был театр, какой жанр пьесы здесь разыгрывается?",
  "Какой танец лучше всего иллюстрирует это движение?",
  "Какая архитектурная форма похожа на эти линии?",
  "Если бы это была мозаика, где её можно было бы увидеть?",
  "Какой мифологический образ скрыт в этих символах?",
  "С каким историческим событием ассоциируется эта картина?",
  "Какой древний город мог бы так выглядеть?",
  "Если бы это был герб, к какому народу он относился бы?",
  "Какая древняя цивилизация могла бы изобразить нечто подобное?",
  "Какой правитель мог бы использовать это изображение как символ?",
  "Какая эпоха в истории лучше всего отражена этим образом?",
  "Если бы это было археологическое открытие, что бы оно значило?",
  "Какая историческая личность могла бы вдохновиться этим образом?",
  "Какую легенду древности напоминает это изображение?",
  "Какие элементы напоминают тебе средневековую символику?",
  "Какое химическое вещество напоминает эта структура?",
  "Может ли это быть моделью атома?",
  "Какая биологическая клетка похожа на это изображение?",
  "Если бы это была планета, какие условия жизни там были бы?",
  "Какая астрономическая гипотеза могла бы объяснить такую форму?",
  "Что здесь похоже на модель молекулы ДНК?",
  "Какая медицинская визуализация могла бы дать такой результат?",
  "Если бы это был спутниковый снимок, что он показывает?",
  "Какая климатическая модель могла бы выглядеть так?",
  "Какая научная гипотеза подтверждается этим образом?",
  "Если бы это было изобретение, то для чего оно предназначено?",
  "Какой гаджет мог бы выглядеть именно так?",
  "Какая архитектурная технология могла бы это создать?",
  "Если бы это был интерфейс, для чего он нужен?",
  "Какая транспортная система могла бы использовать этот принцип?",
  "Что здесь напоминает электрическую схему?",
  "Какое изобретение будущего похоже на это изображение?",
  "Какая часть робота могла бы иметь такую форму?",
  "Какой элемент космического корабля это напоминает?",
  "Если бы это был искусственный интеллект, какие задачи он решал бы?",
  "Какая философская идея лучше всего описывает этот образ?",
  "Какие чувства у тебя вызывает этот сюжет?",
  "Какую экзистенциальную мысль пробуждает это изображение?",
  "Может ли это быть символом свободы?",
  "Что здесь похоже на внутренний мир человека?",
  "Какая эмоция скрыта за этим рисунком?",
  "Какая психологическая теория могла бы объяснить эту картину?",
  "Что это изображение говорит о человеческой душе?",
  "Какая идея о бессознательном здесь выражена?",
  "Что здесь напоминает архетип по Юнгу?",
  "Какая литературная цитата подошла бы сюда?",
  "Какая поэма могла бы быть написана по этому сюжету?",
  "Какой миф или сказка рождаются при виде этой картинки?",
  "Если бы это была обложка книги, какой жанр у неё?",
  "Какая легенда могла бы объяснить происхождение этого места?",
  "Что здесь похоже на сказочный символ?",
  "Какая книга будущего могла бы начинаться с этой картинки?",
  "Какой герой мировой литературы ассоциируется с этим образом?",
  "Какая библейская история напоминает тебе этот сюжет?",
  "Какая пьеса Шекспира могла бы быть связана с этой картиной?",
  "Какие воспоминания вызывает у тебя это изображение?",
  "Если бы это было блюдо, каким оно было бы на вкус?",
  "Какой запах ты представляешь, глядя сюда?",
  "Какая мелодия ассоциируется у тебя с этой картиной?",
  "Что здесь похоже на городскую улицу?",
  "Если бы это был праздник, то какой?",
  "Какая привычка человека напоминает тебе этот сюжет?",
  "Что здесь похоже на детскую игрушку?",
  "Какая бытовая вещь ассоциируется с этой картиной?",
  "Если бы это было кафе, какое меню там подавали бы?",
  "Как это изображение связано с астрономией?",
  "Какая идея из биологии отражена здесь?",
  "Если бы это была компьютерная игра, о чём она?",
  "Что здесь похоже на технологию будущего?",
  "Какая загадка математики напоминает этот сюжет?",
  "Если бы это был сон, что он означает?",
  "Какая энергия ощущается в этой картине?",
  "Какая философская школа могла бы интерпретировать это изображение?",
  "Что здесь похоже на старинный ритуал?",
  "Какая гипотеза о Вселенной отражается в этой картине?",
  "Какая физическая лаборатория могла бы создать такое изображение?",
  "Если бы это был эксперимент, какие выводы он дал бы?",
  "Какая химическая формула напоминает этот узор?",
  "Что здесь похоже на магнитное поле?",
  "Какая идея из квантовой механики связана с этим образом?",
  "Если бы это была модель Вселенной, то какая?",
  "Какая часть атома могла бы выглядеть так?",
  "Что здесь похоже на электрическую цепь?",
  "Какая энергия заключена в этих формах?",
  "Если бы это был робот, для чего он был бы создан?",
  "Какая технология будущего напоминает этот сюжет?",
  "Что здесь похоже на компьютерный интерфейс?",
  "Какая инженерная конструкция могла бы выглядеть так?",
  "Если бы это был спутник, что он исследовал бы?",
  "Какая теория информатики отражена в этом изображении?",
  "Что здесь похоже на алгоритм?",
  "Какая криптографическая схема ассоциируется с этим образом?",
  "Если бы это было изобретение, как бы оно изменило мир?",
  "Какая сеть могла бы работать по такому принципу?",
  "Что здесь похоже на нейросеть?",
  "Если бы это было полотно художника, кто мог бы его написать?",
  "Какая эпоха искусства лучше всего отражена здесь?",
  "Что в этом изображении похоже на импрессионизм?",
  "Какая скульптура могла бы быть создана по мотивам этой картинки?",
  "Если бы это был витраж, где он находился бы?",
  "Какая архитектурная школа ассоциируется с этим образом?",
  "Что здесь напоминает тебе древнегреческий орнамент?",
  "Какая мозаика могла бы повторить этот сюжет?",
  "Если бы это было театральное представление, какой жанр?",
  "Какая опера могла бы начаться с этой сцены?",
  "Что здесь похоже на абстрактное искусство XX века?",
  "Какая картина Сальвадора Дали ближе всего к этому сюжету?",
  "Если бы это был балет, то какой?",
  "Какая художественная техника могла бы передать эту динамику?",
  "Что здесь похоже на современное уличное искусство?",
  "Какая граффити-культура могла бы вдохновиться этим образом?",
  "Если бы это был альбом музыканта, кто бы его выпустил?",
  "Какая иллюстрация к мифу могла бы выглядеть так?",
  "Что здесь похоже на символику эпохи Возрождения?",
  "Какая архитектура будущего отражена в этом изображении?",
  "Какая философская школа могла бы трактовать этот сюжет?",
  "Если бы это был символ экзистенциализма, то почему?",
  "Какая идея о свободе отражена здесь?",
  "Какая теория сознания могла бы объяснить этот сюжет?",
  "Если бы это был архетип Юнга, то какой?",
  "Что здесь похоже на символ бессознательного?",
  "Какая идея о хаосе выражена этим образом?",
  "Если бы это был философский парадокс, то какой?",
  "Какая мысль о времени возникает при виде этой картины?",
  "Что здесь похоже на идею судьбы?",
  "Какая религиозная концепция могла бы объяснить этот сюжет?",
  "Если бы это было учение мудреца, о чём оно?",
  "Какая мораль скрыта за этим изображением?",
  "Что здесь похоже на символ добра и зла?",
  "Какая идея о бесконечности выражена этим сюжетом?",
  "Если бы это было сновидение, что оно значит?",
  "Какая экзистенциальная тревога отражена в этой картине?",
  "Что здесь похоже на внутреннюю гармонию?",
  "Какая древняя цивилизация могла бы оставить такой символ?",
  "Если бы это был герб, какой народ его носил бы?",
  "Какая легенда объяснила бы происхождение этого образа?",
  "Что здесь похоже на миф о создании мира?",
  "Какая мифологическая фигура могла бы быть связана с этим изображением?",
  "Если бы это было предсказание жреца, о чём оно?",
  "Какая историческая эпоха ближе всего к этому сюжету?",
  "Что здесь похоже на символ рыцарской культуры?",
  "Какая мифология могла бы видеть в этом божество?",
  "Если бы это было пророчество, что оно предсказывало бы?",
  "Какая археологическая находка могла бы выглядеть так?",
  "Что здесь похоже на древний календарь?",
  "Какая идея из древнеегипетской символики связана с этим?",
  "Если бы это был амулет, что бы он охранял?",
  "Какая сага или эпос мог бы начинаться с этого образа?",
  "Что здесь похоже на символику племён майя?",
  "Какая история из Библии могла бы ассоциироваться с этим?",
  "Если бы это было знамение на небе, как бы его истолковали?",
  "Какая часть античной мифологии отражена здесь?",
  "Что здесь похоже на средневековый герб?",
  "Если бы это было блюдо, то каким оно было бы?",
  "Какая пряность ассоциируется с этим образом?",
  "Какая привычка человека напоминает этот сюжет?",
  "Если бы это было кафе, какое меню здесь подавали бы?",
  "Какая вещь из быта похожа на этот узор?",
  "Что здесь похоже на витрину магазина?",
  "Какая улица города могла бы выглядеть так?",
  "Какая погода ассоциируется с этим изображением?",
  "Что здесь похоже на сад или парк?",
  "Какая игрушка из детства напоминает эту картину?",
  "Если бы это был модный аксессуар, то какой?",
  "Какая одежда лучше всего отражает этот стиль?",
  "Что здесь похоже на узор ткани?",
  "Какая архитектура интерьера ассоциируется с этим образом?",
  "Если бы это было украшение, то какое?",
  "Какая бытовая привычка связана с этим сюжетом?",
  "Что здесь похоже на коллекцию старинных предметов?",
  "Если бы это был дизайн мебели, то какой?",
];

function LoadingProgressBar() {
  return (
    <>
      <div className="progress">
        <div className="progress-bar progress-bar-striped" role="progressbar"></div>
      </div>
      <style jsx>{`
        .progress-bar {
          width: 0%;
          animation: progressBar 2s linear infinite;
        }

        @keyframes progressBar {
          0% {
            width: 0%;
          }
          100% {
            width: 100%;
          }
        }
      `}</style>
    </>
  );
}

export default function Home() {
  const [defaultQuestion] = useState(
    () => QUESTIONS[Math.floor(Math.random() * QUESTIONS.length)]
  );
  const [accepted, setAccepted] = useState(false);
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [showLicense, setShowLicense] = useState(false);
  const [showResult, setShowResult] = useState(false);
  const [imageSrc, setImageSrc] = useState(null);
  const [imageBlob, setImageBlob] = useState(null);
  const [isPublishing, setIsPublishing] = useState(false);
  const [hintsEnabled, setHintsEnabled] = useState(false);
  const [moodList, setMoodList] = useState([]);
  const [sortAsc, setSortAsc] = useState(false);
  const [page, setPage] = useState(0);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [showPreview, setShowPreview] = useState(false);
  const [previewSrc, setPreviewSrc] = useState(null);
  const [previewError, setPreviewError] = useState(false);
  const [spinClass, setSpinClass] = useState("");
  const listRef = useRef(null);
  const sortAscRef = useRef(sortAsc);

  useEffect(() => {
    const handler = setTimeout(() => setDebouncedQuery(query), 2000);
    return () => clearTimeout(handler);
  }, [query]);

  useEffect(() => {
    sortAscRef.current = sortAsc;
  }, [sortAsc]);

  useEffect(() => {
    setMoodList([]);
    setPage(0);
    setHasMore(true);
  }, [debouncedQuery, defaultQuestion]);

  useEffect(() => {
    setSpinClass(Math.random() < 0.5 ? "spin-cw" : "spin-ccw");
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    const timeout = setTimeout(() => {
      const searchQuery = debouncedQuery === "" ? defaultQuestion : debouncedQuery;
      setLoadingMore(true);
      fetch("https://www.you_site.ru/search", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query: searchQuery,
          topK: (page + 1) * 10,
        }),
        cache: "no-store",
        signal: controller.signal,
      })
        .then((res) => res.json())
        .then(async (data) => {
          const items = data?.hits
            ?.slice(page * 10, (page + 1) * 10)
            ?.map((h) => ({
              text: h.payload.text,
              title: h.payload.title,
              score: h.score,
            }));
          if (!items || items.length === 0) {
            setHasMore(false);
            return;
          }
          const itemsWithImages = await Promise.all(
            items.map(async (item) => {
              try {
                const res = await fetch("https://www.you_site.ru/imager/fetch", {
                  method: "POST",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify({ message_id: item.title }),
                });
                if (!res.ok) {
                  throw new Error("Failed to load image");
                }
                const blob = await res.blob();
                return { ...item, image: URL.createObjectURL(blob) };
              } catch {
                return item;
              }
            })
          );
          setMoodList((prev) => {
            const combined = [...prev, ...itemsWithImages];
            return combined.sort((a, b) =>
              sortAscRef.current ? a.score - b.score : b.score - a.score
            );
          });
          if (items.length < 10) {
            setHasMore(false);
          }
        })
        .catch(() => setHasMore(false))
        .finally(() => setLoadingMore(false));
    }, 200);
    return () => {
      controller.abort();
      clearTimeout(timeout);
    };
  }, [page, debouncedQuery, defaultQuestion]);

  const handleSortToggle = () => {
    const newSortAsc = !sortAsc;
    const sorted = [...moodList].sort((a, b) =>
      newSortAsc ? a.score - b.score : b.score - a.score
    );
    setMoodList(sorted);
    setSortAsc(newSortAsc);
  };

  const handleScroll = () => {
    if (!listRef.current || loadingMore || !hasMore) return;
    const { scrollTop, clientHeight, scrollHeight } = listRef.current;
    if (scrollTop + clientHeight >= scrollHeight - 50) {
      setPage((p) => p + 1);
    }
  };

  const isSearchDisabled = !accepted || query.trim() === "";

  const handleSearch = () => {
    setShowResult(true);
    setImageSrc(null);
    setImageBlob(null);
    fetch("https://www.you_site.ru/api/images/png", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        prompt: query,
        size: "1024x1024",
        model: "dall-e-3",
      }),
    })
      .then((res) => res.blob())
      .then((blob) => {
        setImageSrc(URL.createObjectURL(blob));
        setImageBlob(blob);
      });
  };

  const speak = (text) => {
    if ("speechSynthesis" in window) {
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = "ru-RU";
      utterance.rate = 1;
      utterance.pitch = 1;
      window.speechSynthesis.speak(utterance);
    }
  };

  const showHintImage = (title) => {
    setShowPreview(true);
    setPreviewSrc(null);
    setPreviewError(false);
    fetch("https://www.you_site.ru/imager/fetch", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message_id: title }),
    })
      .then((res) => {
        if (!res.ok) {
          throw new Error("Failed to load image");
        }
        return res.blob();
      })
      .then((blob) => {
        setPreviewSrc(URL.createObjectURL(blob));
      })
      .catch(() => {
        setPreviewError(true);
      });
  };

  return (
    <div
      className="container d-flex flex-column pt-5"
      style={{ height: "100vh", overflow: "hidden" }}
    >
      <div className="d-flex align-items-center justify-content-center mb-4 gap-2">
        <a href="https://www.you_site.ru" target="_self">
          <Image
            src="/logo.svg"
            alt="Logo"
            width={32}
            height={32}
            className={`logo ${spinClass}`}
          />
        </a>
        <h1 className="mb-0" style={{ lineHeight: "32px" }}>
          <a href="https://t.me/YouKanal">AI-публикатор</a>
        </h1>
      </div>
      <div className="mb-3">
        <SearchBox
          value={query}
          onChange={setQuery}
          disabled={!accepted}
          onSearch={handleSearch}
          searchDisabled={isSearchDisabled}
          hintsEnabled={hintsEnabled}
        />
      </div>
      <div className="mb-3">
        <div className="form-check form-check-inline">
          <input
            type="checkbox"
            className="form-check-input"
            id="licenseCheck"
            checked={accepted}
            onChange={(e) => setAccepted(e.target.checked)}
          />
          <label className="form-check-label" htmlFor="licenseCheck">
            Согласен с{" "}
            <a
              href="#"
              onClick={(e) => {
                e.preventDefault();
                setShowLicense(true);
              }}
            >
              Лицензией
            </a>
          </label>
        </div>
        <div className="form-check form-check-inline">
          <input
            type="checkbox"
            className="form-check-input"
            id="hintsCheck"
            checked={hintsEnabled}
            onChange={(e) => setHintsEnabled(e.target.checked)}
          />
          <label className="form-check-label" htmlFor="hintsCheck">
            Подсказки
          </label>
        </div>
      </div>

      <div
        className="flex-grow-1 overflow-auto"
        onScroll={handleScroll}
        ref={listRef}
      >
        {moodList.length > 0 && (
          <div className="mb-3">
            <h5>
              {query ? "Поиск..." : defaultQuestion}
              <span
                className="ms-2"
                style={{ cursor: "pointer" }}
                onClick={handleSortToggle}
              >
                {sortAsc ? "▲" : "▼"}
              </span>
            </h5>
            <ul>
              {moodList.map((t, i) => (
                <li key={i} className="mb-2 clearfix">
                  {t.image && (
                    <img
                      src={t.image}
                      alt=""
                      width={64}
                      height={64}
                      className="float-start me-2 mb-1"
                      style={{ objectFit: "contain", cursor: "pointer" }}
                      onClick={() => showHintImage(t.title)}
                    />
                  )}
                  <span>{t.text}</span>
                  <button
                    type="button"
                    className="btn btn-link btn-sm p-0 text-decoration-none"
                    onClick={() => speak(t.text)}
                    aria-label="Озвучить подсказку"
                  >
                    🔊
                  </button>
                  {t.image && (
                    <button
                      type="button"
                      className="btn btn-link btn-sm p-0 text-decoration-none ms-1"
                      onClick={() => showHintImage(t.title)}
                      aria-label="Показать изображение"
                    >
                      🔗
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}
        {loadingMore && (
          <div className="text-center py-2">Загрузка...</div>
        )}
      </div>

      {showPreview && (
        <>
          <div className="modal show d-block" tabIndex="-1">
            <div className="modal-dialog">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Изображение</h5>
                </div>
                <div className="modal-body">
                  <div className="text-center">
                    {previewError ? (
                      <p>Изображение не найдено</p>
                    ) : previewSrc ? (
                      <img
                        src={previewSrc}
                        alt="Подсказка"
                        className="img-fluid"
                        onError={() => setPreviewError(true)}
                      />
                    ) : (
                      <LoadingProgressBar />
                    )}
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={() => setShowPreview(false)}
                  >
                    Закрыть
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div className="modal-backdrop fade show"></div>
        </>
      )}

      {showLicense && (
        <>
          <div className="modal show d-block" tabIndex="-1">
            <div className="modal-dialog">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Лицензионное соглашение</h5>
                </div>
                <div className="modal-body">
                  <p>Любые результаты поиска являются Вашей собственностью.</p>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={() => setShowLicense(false)}
                  >
                    ОК
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div className="modal-backdrop fade show"></div>
        </>
      )}

      {showResult && (
        <>
          <div className="modal show d-block" tabIndex="-1">
            <div className="modal-dialog">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Результат поиска</h5>
                </div>
                <div className="modal-body">
                  <p>{query}</p>
                  <div className="text-center mt-3">
                    {imageSrc ? (
                      <img src={imageSrc} alt="Результат" className="img-fluid" />
                    ) : (
                      <LoadingProgressBar />
                    )}
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className={`btn ${imageSrc ? "btn-primary" : "btn-secondary"}`}
                    disabled={!imageSrc || isPublishing}
                    onClick={() => {
                      if (!imageBlob || isPublishing) return;
                      setIsPublishing(true);
                      const formData = new FormData();
                      formData.append("image", imageBlob, "image.png");
                      formData.append("caption", query);
                      fetch("https://www.you_site.ru/publish", {
                        method: "POST",
                        body: formData,
                      }).finally(() => {
                        setIsPublishing(false);
                        setShowResult(false);
                      });
                    }}
                  >
                    Опубликовать
                  </button>
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={() => setShowResult(false)}
                  >
                    Закрыть
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div className="modal-backdrop fade show"></div>
        </>
      )}
      <style jsx>{`
        .logo {
          display: block;
          transform-origin: center;
        }
        .spin-cw {
          animation: spin 10s linear infinite;
        }
        .spin-ccw {
          animation: spin 10s linear infinite reverse;
        }
        @keyframes spin {
          from {
            transform: rotate(0deg);
          }
          to {
            transform: rotate(360deg);
          }
        }
      `}</style>
    </div>
  );
}
