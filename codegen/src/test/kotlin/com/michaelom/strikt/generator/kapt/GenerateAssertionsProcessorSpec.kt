package com.michaelom.strikt.generator.kapt

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import org.intellij.lang.annotations.Language
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import strikt.api.*
import strikt.assertions.*
import java.io.File

@KotlinPoetMetadataPreview
object GenerateAssertionsProcessorSpec : Spek({

    describe("assertion generation") {

        context("if the annotated type is a data class") {

            it("generates assertions for simple types [Int, String]") {
                val compilation = compileSources("Car.kt")
                expectThat(compilation.exitCode).isEqualTo(ExitCode.OK)

                @Language("kotlin")
                val expected = """
                    package com.michaelom.strikt.generator.kapt.sources
                    
                    import kotlin.Int
                    import kotlin.String
                    import strikt.api.Assertion
                    import strikt.api.Assertion.Builder
                    
                    val Assertion.Builder<Car>.make: Assertion.Builder<String>
                      get() = get("make", Car::make)
                    val Assertion.Builder<Car>.year: Assertion.Builder<Int>
                      get() = get("year", Car::year)
                """.trimIndent()

                expectThat(compilation.assertionFile())
                    .isNotNull()
                    .equalsLineByLine(expected)
            }

            it("generates assertions for optional types") {
                val compilation = compileSources("CarWithOptionalYear.kt")
                expectThat(compilation.exitCode).isEqualTo(ExitCode.OK)

                @Language("kotlin")
                val expected = """
                    package com.michaelom.strikt.generator.kapt.sources
                    
                    import kotlin.Int
                    import kotlin.String
                    import strikt.api.Assertion
                    import strikt.api.Assertion.Builder
                    
                    val Assertion.Builder<CarWithOptionalYear>.make: Assertion.Builder<String>
                      get() = get("make", CarWithOptionalYear::make)
                    val Assertion.Builder<CarWithOptionalYear>.year: Assertion.Builder<Int?>
                      get() = get("year", CarWithOptionalYear::year)
                """.trimIndent()

                expectThat(compilation.assertionFile())
                    .isNotNull()
                    .equalsLineByLine(expected)
            }

            it("generates assertions for nested properties") {
                val compilation = compileSources("Person.kt", "Car.kt", "Sex.kt")
                expectThat(compilation.exitCode).isEqualTo(ExitCode.OK)

                @Language("kotlin")
                val expected = """
                    package com.michaelom.strikt.generator.kapt.sources
                    
                    import kotlin.String
                    import kotlin.Unit
                    import strikt.api.Assertion
                    import strikt.api.Assertion.Builder
                    
                    val Assertion.Builder<Person>.car: Assertion.Builder<Car>
                      get() = get("car", Person::car)
                    fun Assertion.Builder<Person>.car(block: Assertion.Builder<Car>.() -> Unit):
                        Assertion.Builder<Person> = with(function = Person::car, block = block)
                    
                    val Assertion.Builder<Person>.name: Assertion.Builder<String>
                      get() = get("name", Person::name)
                    val Assertion.Builder<Person>.sex: Assertion.Builder<Sex>
                      get() = get("sex", Person::sex)
                """.trimIndent()

                expectThat(compilation.assertionFile("PersonAssertions.kt"))
                    .isNotNull()
                    .equalsLineByLine(expected)
            }

            it("generates assertions for nullable nested properties") {
                val compilation = compileSources("PersonWithNullableCar.kt", "Car.kt", "Sex.kt")
                expectThat(compilation.exitCode).isEqualTo(ExitCode.OK)

                @Language("kotlin")
                val expected = """
                    package com.michaelom.strikt.generator.kapt.sources

                    import kotlin.Unit
                    import strikt.api.Assertion
                    import strikt.api.Assertion.Builder
                    import strikt.assertions.isNotNull
                    
                    val Assertion.Builder<PersonWithNullableCar>.car: Assertion.Builder<Car?>
                      get() = get("car", PersonWithNullableCar::car)
                    fun Assertion.Builder<PersonWithNullableCar>.car(block: Assertion.Builder<Car>.() -> Unit):
                        Assertion.Builder<PersonWithNullableCar> = with(PersonWithNullableCar::car) {
                        isNotNull().and(block) }
                """.trimIndent()

                expectThat(compilation.assertionFile("PersonWithNullableCarAssertions.kt"))
                    .isNotNull()
                    .equalsLineByLine(expected)
            }

            it("skips private properties") {
                val compilation = compileSources("TypeWithPrivateProperties.kt")
                expectThat(compilation.exitCode).isEqualTo(ExitCode.OK)

                @Language("kotlin")
                val expected = """
                    package com.michaelom.strikt.generator.kapt.sources
                  
                    import kotlin.String
                    import strikt.api.Assertion
                    import strikt.api.Assertion.Builder
                  
                    val Assertion.Builder<TypeWithPrivateProperties>.public: Assertion.Builder<String>
                      get() = get("public", TypeWithPrivateProperties::public)
                """.trimIndent()

                expectThat(compilation.assertionFile("TypeWithPrivatePropertiesAssertions.kt"))
                    .isNotNull()
                    .equalsLineByLine(expected)
            }

            it("treats nested classes as top level classes") {
                val compilation = compileSources("NestedClass.kt")
                expectThat(compilation.exitCode).isEqualTo(ExitCode.OK)

                @Language("kotlin")
                val expected = """
                    package com.michaelom.strikt.generator.kapt.sources
                  
                    import kotlin.String
                    import strikt.api.Assertion
                    import strikt.api.Assertion.Builder
                  
                    val Assertion.Builder<TopLevel.Nested>.property: Assertion.Builder<String>
                      get() = get("property", TopLevel.Nested::property)
                """.trimIndent()

                expectThat(compilation.assertionFile("TopLevel.NestedAssertions.kt"))
                    .isNotNull()
                    .equalsLineByLine(expected)
            }

            it("creates one assertion file for each top level class") {
                val compilation = compileSources("MultipleTopLevelClasses.kt")
                expectThat(compilation.exitCode).isEqualTo(ExitCode.OK)

                val assertionFiles = compilation.assertionFiles()
                expectThat(assertionFiles)
                    .hasSize(3)
                    .map { it.name }
                    .containsExactlyInAnyOrder("AAssertions.kt", "BAssertions.kt", "CAssertions.kt")
            }

            it("ignores companion object properties") {
                val compilation = compileSources("CompanionProperties.kt")
                expectThat(compilation.exitCode).isEqualTo(ExitCode.OK)

                @Language("kotlin")
                val expected = """
                    package com.michaelom.strikt.generator.kapt.sources
                    
                    import kotlin.String
                    import strikt.api.Assertion
                    import strikt.api.Assertion.Builder
                    
                    val Assertion.Builder<CompanionProperties>.property: Assertion.Builder<String>
                      get() = get("property", CompanionProperties::property)
                """.trimIndent()

                expectThat(compilation.assertionFile("CompanionPropertiesAssertions.kt"))
                    .isNotNull()
                    .equalsLineByLine(expected)
            }

            it("supports iterables") {
                val compilation = compileSources("Iterables.kt")
                expectThat(compilation.exitCode).isEqualTo(ExitCode.OK)

                @Language("kotlin")
                val expected = """
                    package com.michaelom.strikt.generator.kapt.sources
                    
                    import kotlin.String
                    import kotlin.collections.Collection
                    import kotlin.collections.Iterable
                    import kotlin.collections.List
                    import kotlin.collections.Map
                    import kotlin.collections.Set
                    import strikt.api.Assertion
                    import strikt.api.Assertion.Builder
                    
                    val Assertion.Builder<Iterables>.collection: Assertion.Builder<Collection<String>>
                      get() = get("collection", Iterables::collection)
                    val Assertion.Builder<Iterables>.iterable: Assertion.Builder<Iterable<String>>
                      get() = get("iterable", Iterables::iterable)
                    val Assertion.Builder<Iterables>.list: Assertion.Builder<List<String>>
                      get() = get("list", Iterables::list)
                    val Assertion.Builder<Iterables>.map: Assertion.Builder<Map<String, String>>
                      get() = get("map", Iterables::map)
                    val Assertion.Builder<Iterables>.set: Assertion.Builder<Set<String>>
                      get() = get("set", Iterables::set)
                """.trimIndent()

                expectThat(compilation.assertionFile("IterablesAssertions.kt"))
                    .isNotNull()
                    .equalsLineByLine(expected)
            }
        }

        it("supports sequences") {
            val compilation = compileSources("Sequences.kt")
            expectThat(compilation.exitCode).isEqualTo(ExitCode.OK)

            @Language("kotlin")
            val expected = """
                package com.michaelom.strikt.generator.kapt.sources

                import kotlin.String
                import kotlin.sequences.Sequence
                import strikt.api.Assertion
                import strikt.api.Assertion.Builder
                
                val Assertion.Builder<Sequences>.sequence: Assertion.Builder<Sequence<String>>
                  get() = get("sequence", Sequences::sequence)
                """.trimIndent()

            expectThat(compilation.assertionFile("SequencesAssertions.kt"))
                .isNotNull()
                .equalsLineByLine(expected)
        }

        context("if the annotated type is not a data class") {

            listOf("NormalClass", "Interface", "AbstractClass").forEach { name ->

                it("does not generate assertions for '$name'") {
                    val compilation = compileSources("$name.kt")

                    expectThat(compilation.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
                    expectThat(compilation.messages)
                        .contains("@GenerateAssertions can't be applied to " +
                            "com.michaelom.strikt.generator.kapt.sources.$name: must be a Kotlin data class")
                }
            }

            it("ignores companion objects") {
                val compilation = compileSources("Companion.kt")

                expectThat(compilation.exitCode).isEqualTo(ExitCode.OK)
                expectThat(compilation.assertionFiles()).isEmpty()
            }

            it("does not generate assertions for private classes") {
                val compilation = compileSources("PrivateClass.kt")

                expectThat(compilation.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
                expectThat(compilation.messages)
                    .contains("Error processing com.michaelom.strikt.generator.kapt.sources.PrivateClass: " +
                        "@GenerateAssertions can't be applied to private classes")
            }
        }

        context("when the annotated data class has supertypes") {

            it("ignores properties on supertypes") {
                val compilation = compileSources("TypeWithSuperTypes.kt")
                expectThat(compilation.exitCode).isEqualTo(ExitCode.OK)

                @Language("kotlin")
                val expected = """
                    package com.michaelom.strikt.generator.kapt.sources
                    
                    import kotlin.Int
                    import strikt.api.Assertion
                    import strikt.api.Assertion.Builder
                    
                    val Assertion.Builder<TypeWithSuperTypes>.someInt: Assertion.Builder<Int>
                      get() = get("someInt", TypeWithSuperTypes::someInt)
                """.trimIndent()

                expectThat(compilation.assertionFile("TypeWithSuperTypesAssertions.kt"))
                    .isNotNull()
                    .equalsLineByLine(expected)
            }
        }
    }

})

private fun Assertion.Builder<File>.equalsLineByLine(expected: String): Assertion.Builder<List<String>> {
    return lines().isEqualTo(expected.lines())
}
