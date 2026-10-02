package com.example.calibretv.data.quiz

import kotlinx.serialization.Serializable

@Serializable
data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String = ""
)

@Serializable
data class BookQuiz(
    val bookTitle: String,
    val questions: List<QuizQuestion>
)

object QuizRepository {

    private val curatedQuizzes = mapOf(
        "el principito" to BookQuiz(
            bookTitle = "El Principito",
            questions = listOf(
                QuizQuestion(
                    id = "p1",
                    question = "¿Qué dibujo le pide el Principito al aviador al conocerlo?",
                    options = listOf("Un cordero", "Una flor", "Un avión"),
                    correctIndex = 0,
                    explanation = "El Principito le dice insistentemente: «Por favor... ¡dibújame un cordero!»"
                ),
                QuizQuestion(
                    id = "p2",
                    question = "¿Cuál es el secreto que el zorro le revela al Principito?",
                    options = listOf(
                        "Las estrellas brillan para todos",
                        "Lo esencial es invisible a los ojos",
                        "El tiempo no se puede detener"
                    ),
                    correctIndex = 1,
                    explanation = "«Solo con el corazón se puede ver bien; lo esencial es invisible a los ojos»."
                ),
                QuizQuestion(
                    id = "p3",
                    question = "¿Qué planta peligrosa debía limpiar el Principito a diario de su asteroide?",
                    options = listOf("Los baobabs", "Las zarzas", "Los cactus"),
                    correctIndex = 0,
                    explanation = "Los baobabs podían perforar y destruir su pequeño asteroide B-612."
                )
            )
        ),
        "alicia en el pais de las maravillas" to BookQuiz(
            bookTitle = "Alicia en el País de las Maravillas",
            questions = listOf(
                QuizQuestion(
                    id = "a1",
                    question = "¿A qué animal persigue Alicia al inicio de su aventura?",
                    options = listOf("Un gato sonriente", "Un conejo blanco con reloj", "Una oruga azul"),
                    correctIndex = 1,
                    explanation = "Alicia vio pasar a un conejo blanco con chaleco y reloj de bolsillo exclamando que llegaba tarde."
                ),
                QuizQuestion(
                    id = "a2",
                    question = "¿Qué personaje celebra constantemente su «no-cumpleaños»?",
                    options = listOf("El Sombrerero Loco", "La Reina de Corazones", "El Conejo Blanco"),
                    correctIndex = 0,
                    explanation = "El Sombrerero Loco y la Liebre de Marzo celebran una merienda interminable."
                ),
                QuizQuestion(
                    id = "a3",
                    question = "¿Qué frase gritaba furiosa la Reina de Corazones?",
                    options = listOf("¡A prisiones con él!", "¡Que le corten la cabeza!", "¡Fuera de mi reino!"),
                    correctIndex = 1,
                    explanation = "La orden favorita de la Reina de Corazones ante cualquier disgusto era: «¡Que le corten la cabeza!»"
                )
            )
        ),
        "la isla del tesoro" to BookQuiz(
            bookTitle = "La Isla del Tesoro",
            questions = listOf(
                QuizQuestion(
                    id = "t1",
                    question = "¿Cómo se llama el joven protagonista y narrador de la historia?",
                    options = listOf("Jim Hawkins", "Long John Silver", "Billy Bones"),
                    correctIndex = 0,
                    explanation = "Jim Hawkins es el hijo del posadero de «El Almirante Benbow»."
                ),
                QuizQuestion(
                    id = "t2",
                    question = "¿Qué tenía el pirata Long John Silver como mascota?",
                    options = listOf("Un mono llamado Jack", "Un loro llamado Capitán Flint", "Un halcón negro"),
                    correctIndex = 1,
                    explanation = "Llevaba al hombro un loro verde que gritaba: «¡Piezas de a ocho!»"
                ),
                QuizQuestion(
                    id = "t3",
                    question = "¿Qué señal temida recibían los piratas como sentencia de muerte?",
                    options = listOf("La mancha negra", "La calavera de plata", "Una moneda rota"),
                    correctIndex = 0,
                    explanation = "La marca o mancha negra era entregada en secreto a los piratas juzgados por la tripulación."
                )
            )
        ),
        "sherlock holmes" to BookQuiz(
            bookTitle = "Las Aventuras de Sherlock Holmes",
            questions = listOf(
                QuizQuestion(
                    id = "s1",
                    question = "¿En qué dirección de Londres vivía Sherlock Holmes?",
                    options = listOf("221B Baker Street", "10 Downing Street", "45 Fleet Street"),
                    correctIndex = 0,
                    explanation = "El famoso detective residía junto al Dr. Watson en el 221B de Baker Street."
                ),
                QuizQuestion(
                    id = "s2",
                    question = "¿Qué instrumento musical tocaba Holmes para concentrarse?",
                    options = listOf("El violonchelo", "El violín", "El piano"),
                    correctIndex = 1,
                    explanation = "Sherlock Holmes tocaba el violín en sus horas de profunda reflexión deductiva."
                ),
                QuizQuestion(
                    id = "s3",
                    question = "¿Quién es el leal compañero y cronista de sus casos?",
                    options = listOf("El Inspector Lestrade", "El Doctor John H. Watson", "Mycroft Holmes"),
                    correctIndex = 1,
                    explanation = "El Dr. Watson acompaña y registra en papel todas las investigaciones de Holmes."
                )
            )
        )
    )

    fun getQuizForBook(title: String): BookQuiz {
        val normalized = title.lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")

        for ((key, quiz) in curatedQuizzes) {
            if (normalized.contains(key)) {
                return quiz
            }
        }

        // Generic reading comprehension quiz for other books
        return BookQuiz(
            bookTitle = title,
            questions = listOf(
                QuizQuestion(
                    id = "g1",
                    question = "¿Cuál dirías que es el tema o conflicto principal de esta lectura?",
                    options = listOf(
                        "La superación de un gran desafío o misterio",
                        "Una guía puramente técnica de matemáticas",
                        "Un listado de compras cotidianas"
                    ),
                    correctIndex = 0,
                    explanation = "Las historias literarias giran en torno a metas y obstáculos de los personajes."
                ),
                QuizQuestion(
                    id = "g2",
                    question = "¿Cómo reaccionan los protagonistas frente a las dificultades presentadas?",
                    options = listOf(
                        "Se rinden al primer instante",
                        "Buscan soluciones con ingenio, valor o la ayuda de otros",
                        "Ignoran por completo lo que sucede"
                    ),
                    correctIndex = 1,
                    explanation = "El avance de la trama se sostiene en el ingenio y determinación de los personajes."
                ),
                QuizQuestion(
                    id = "g3",
                    question = "¿Qué valor o enseñanza destaca en lo leído?",
                    options = listOf(
                        "La curiosidad, el aprendizaje y la perseverancia",
                        "La indiferencia ante el entorno",
                        "El miedo a explorar cosas nuevas"
                    ),
                    correctIndex = 0,
                    explanation = "La literatura enriquece el pensamiento crítico y la curiosidad por el mundo."
                )
            )
        )
    }
}
