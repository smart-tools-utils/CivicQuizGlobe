package com.example.data.ai

import com.example.data.model.QuizItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Service for generating Swedish Citizenship (Medborgarskapstest) flashcards with AI.
 */
object AiCardGenerator {

    data class GeneratedQuiz(
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String
    )

    private val presetQuestionsByTopic = mapOf(
        "Sveriges grundlagar & demokrati" to listOf(
            GeneratedQuiz(
                question = "Vad slår den första meningen i Regeringsformen fast?",
                options = listOf(
                    "All offentlig makt i Sverige utgår från folket",
                    "Kungen bestämmer ensam över lagstiftningen",
                    "Regeringen kan ändra grundlagar utan riksdagens godkännande",
                    "Sverige styrs direkt av EU-kommissionen"
                ),
                correctIndex = 0,
                explanation = "Regeringsformen 1 kap. 1 § inleds med: 'All offentlig makt i Sverige utgår från folket. Den svenska folkstyrelsen bygger på fri åsiktsbildning och på allmän och lika rösträtt.'"
            ),
            GeneratedQuiz(
                question = "Hur ändras en svensk grundlag?",
                options = listOf(
                    "Genom två likalydande riksdagsbeslut med ett allmänt riksdagsval emellan",
                    "Genom ett enda beslut i regeringen",
                    "Bara genom beslut av Högsta domstolen",
                    "Grundlagar kan aldrig ändras"
                ),
                correctIndex = 0,
                explanation = "För att ändra en grundlag krävs att riksdagen fattar två likadana beslut, och mellan dessa två beslut måste det hållas ett ordinarie allmänt riksdagsval så att väljarna kan säga sitt."
            ),
            GeneratedQuiz(
                question = "Vilken grundlag reglerar vem som får bli kung eller regerande drottning i Sverige?",
                options = listOf(
                    "Successionsordningen (SO)",
                    "Regeringsformen",
                    "Tryckfrihetsförordningen",
                    "Yttrandefrihetsgrundlagen"
                ),
                correctIndex = 0,
                explanation = "Successionsordningen reglerar tronföljden inom ätten Bernadotte och fastställdes 1810 med ändring till kognatisk tronföljd (äldsta barnet oavsett kön) 1980."
            )
        ),
        "Riksdagen & Regeringen" to listOf(
            GeneratedQuiz(
                question = "Vad krävs för att ett politiskt parti ska få mandat i Sveriges riksdag (riksdagsspärren)?",
                options = listOf(
                    "Minst 4 % av rösterna i hela landet (eller 12 % i en enskild valkrets)",
                    "Minst 10 % av rösterna i hela landet",
                    "Minst 25 % av rösterna i Stockholm",
                    "Ingen spärr finns, alla registrerade partier får plats"
                ),
                correctIndex = 0,
                explanation = "Sverige tillämpar en fyraprocentsspärr (4 %) för att förhindra alltför stor splittring i parlamentet och skapa stabila regeringsunderlag."
            ),
            GeneratedQuiz(
                question = "Vem föreslår och utser Sveriges statsminister?",
                options = listOf(
                    "Riksdagens talman föreslår kandidat som därefter röstas fram av riksdagen",
                    "Kungen utser statsministern på eget initiativ",
                    "Folket väljer statsministern i ett separat direktval",
                    "Högsta förvaltningsdomstolen utser statsministern"
                ),
                correctIndex = 0,
                explanation = "I Sverige föreslår riksdagens talman en statsministerkandidat efter överläggningar med partiledarna. Riksdagen röstar sedan om förslaget enligt principen om negativ parlamentarism."
            ),
            GeneratedQuiz(
                question = "Vad är Konstitutionsutskottets (KU) uppgift i riksdagen?",
                options = listOf(
                    "Att granska att ministrarna och regeringen följer lagarna och sköter sina uppgifter",
                    "Att besluta om räntan hos Riksbanken",
                    "Att leda polisens utredningar",
                    "Att förhandla om Sveriges internationella handelsavtal"
                ),
                correctIndex = 0,
                explanation = "Konstitutionsutskottet (KU) är ett riksdagsutskott vars uppgift är att granska att regeringen och ministrarna följer grundlagarna och reglementena."
            )
        ),
        "Svenska samhället & välfärd" to listOf(
            GeneratedQuiz(
                question = "Vilken typ av beskattning utgör grunden för finansieringen av svensk skola, vård och omsorg?",
                options = listOf(
                    "Den kommunala och regionala inkomstskatten samt moms och arbetsgivaravgifter",
                    "Enbart tullavgifter på importerade varor",
                    "Frivilliga donationer från storföretag",
                    "Enbart avgifter från utländska turister"
                ),
                correctIndex = 0,
                explanation = "Den svenska välfärdsmodellen finansieras huvudsakligen via skatter, där den kommunala inkomstskatten finansierar skola och äldreomsorg, och regionskatten finansierar sjukvården."
            ),
            GeneratedQuiz(
                question = "Vad innebär den allmänna skolplikten i Sverige?",
                options = listOf(
                    "Alla barn mellan 6 och 16 år (förskoleklass till årskurs 9) har laglig rätt och plikt till utbildning",
                    "Endast barn med svenskt medborgarskap måste gå i skolan",
                    "Skolan är valfri för alla familjer",
                    "Skolplikten gäller enbart fram till 12 års ålder"
                ),
                correctIndex = 0,
                explanation = "I Sverige gäller skolplikt från förskoleklass (6 års ålder) till slutet av årskurs 9 i grundskolan. Utbildningen är avgiftsfri för alla."
            )
        ),
        "Rättigheter & allemansrätten" to listOf(
            GeneratedQuiz(
                question = "Får man elda i naturen enligt allemansrätten?",
                options = listOf(
                    "Ja, om man iakttar stor försiktighet och det inte råder eldningsförbud",
                    "Nej, det är alltid strängt förbjudet att göra upp eld i Sverige",
                    "Ja, man får elda överallt inklusive direkt på klipphällar",
                    "Endast om man har ett skriftligt tillstånd från regeringen"
                ),
                correctIndex = 0,
                explanation = "Man får göra upp en liten eld under säkra förhållanden om det inte finns risk för spridning och inte råder eldningsförbud från länsstyrelse eller räddningstjänst. Man ska dock aldrig elda direkt på klippor då de spricker."
            ),
            GeneratedQuiz(
                question = "Vad skyddar Diskrimineringslagen i Sverige?",
                options = listOf(
                    "Skyddar individer mot orättvis behandling baserad på kön, etnicitet, religion, funktionsnedsättning, sexuell läggning eller ålder",
                    "Gäller endast inom militären",
                    "Skyddar enbart statliga myndigheter",
                    "Gäller bara vid bilköp och privata avtal"
                ),
                correctIndex = 0,
                explanation = "Diskrimineringslagen motverkar diskriminering och främjar lika rättigheter oavsett kön, könsöverskridande identitet, etnisk tillhörighet, religion, funktionsnedsättning, sexuell läggning eller ålder."
            )
        ),
        "Geografi & historia" to listOf(
            GeneratedQuiz(
                question = "Vilka är Sveriges tre traditionella landsdelar?",
                options = listOf(
                    "Götaland, Svealand och Norrland",
                    "Östergötland, Västergötland och Skåneland",
                    "Lappland, Småland och Uppland",
                    "Höglandet, Kustlandet och Fjällvärlden"
                ),
                correctIndex = 0,
                explanation = "Sverige är traditionellt uppdelat i tre historiska landsdelar: Götaland i söder, Svealand i mitten och Norrland i norr."
            ),
            GeneratedQuiz(
                question = "Vilken är Sveriges största insjö och den tredje största i Europa?",
                options = listOf(
                    "Vänern",
                    "Vättern",
                    "Mälaren",
                    "Hjälmaren"
                ),
                correctIndex = 0,
                explanation = "Vänern är Sveriges största sjö till ytan och den största insjön inom EU."
            )
        )
    )

    val availableTopics = listOf(
        "Sveriges grundlagar & demokrati",
        "Riksdagen & Regeringen",
        "Svenska samhället & välfärd",
        "Rättigheter & allemansrätten",
        "Geografi & historia"
    )

    /**
     * Generates a new quiz question based on requested topic.
     */
    suspend fun generateQuizItem(topic: String): QuizItem = withContext(Dispatchers.Default) {
        delay(900)

        val candidateList = presetQuestionsByTopic[topic]
            ?: presetQuestionsByTopic.values.flatten()

        val selected = candidateList.random()
        val currentTime = System.currentTimeMillis()

        QuizItem(
            id = 0,
            question = selected.question,
            options = selected.options,
            correctIndex = selected.correctIndex,
            explanation = selected.explanation,
            interval = 0,
            repetition = 0,
            easeFactor = 2.5f,
            nextReviewDate = currentTime
        )
    }
}
