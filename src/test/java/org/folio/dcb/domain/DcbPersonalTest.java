package org.folio.dcb.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class DcbPersonalTest {

  private static final DcbPersonal DEFAULT_VALUE = patronInfo(null, null, "DcbSystem");

  @ParameterizedTest
  @MethodSource("validInputs")
  void parseLocalNames_parameterized_validValues(String input, DcbPersonal expected) {
    var result = DcbPersonal.parseLocalNames(input);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void parseLocalNames_positive_nullInput() {
    var result = DcbPersonal.parseLocalNames(null);
    assertThat(result).isEqualTo(DEFAULT_VALUE);
  }

  @ValueSource(strings = {
    "",
    " ",
    "   ",
    "[]",
    "[,,]",
    "[ ,  ,  ]",
    "[ ,  ,  ,  ,  ,]",
    "[John, Michael, , Doe]",
    "[John, Michael, , , Doe]",
    "[a, b, c, d, e, f]",
  })
  @ParameterizedTest
  void parseLocalNames_parameterized_blankValues(String input) {
    var result = DcbPersonal.parseLocalNames(input);
    assertThat(result).isEqualTo(DEFAULT_VALUE);
  }

  @ValueSource(strings = {
    "[",
    "]",
    "random text",
    "John, Michael, Doe",
    "John, Michael, Doe]",
    "[John, Michael, Doe",
  })
  @ParameterizedTest
  void parseLocalNames_parameterized_invalidValues(String input) {
    assertThatThrownBy(() -> DcbPersonal.parseLocalNames(input))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Malformed localNames format. Value must start with '[' and end with ']'");
  }

  @Test
  void factoryMethod_shouldCreateInstance() {
    var result = new DcbPersonal("John", "M", "Doe");
    assertThat(result.getFirstName()).isEqualTo("John");
    assertThat(result.getMiddleName()).isEqualTo("M");
    assertThat(result.getLastName()).isEqualTo("Doe");
  }

  static Stream<Arguments> validInputs() {
    return Stream.of(
      arguments("[John,Michael,Doe]", patronInfo("John", "Michael", "Doe")),
      arguments("[John, Michael, Doe]", patronInfo("John", "Michael", "Doe")),
      arguments("[John, John, Doe]", patronInfo("John", "John", "Doe")),
      arguments("[John, , Doe]", patronInfo("John", null, "Doe")),
      arguments("[John,, Doe]", patronInfo("John", null, "Doe")),
      arguments("[ , , Doe]", patronInfo(null, null, "Doe")),
      arguments("[,, Doe]", patronInfo(null, null, "Doe")),
      arguments("[A, B, C]", patronInfo("A", "B", "C")),
      arguments("[  John  ,  Michael  ,  Doe  ]", patronInfo("John", "Michael", "Doe")),
      arguments("[John,Michael,Doe]", patronInfo("John", "Michael", "Doe")),
      arguments("[John Paul, Maria Elena, Van Der Berg]",
        patronInfo("John Paul", "Maria Elena", "Van Der Berg")),
      arguments("[Jean-Pierre, Marie-Claire, O'Connor]",
        patronInfo("Jean-Pierre", "Marie-Claire", "O'Connor")),
      arguments("[José, María, González]", patronInfo("José", "María", "González")),
      arguments("[\tJohn\t, \nMichael\n, \rDoe\r]", patronInfo("John", "Michael", "Doe")),
      arguments("[a,b,c]", patronInfo("a", "b", "c")),
      arguments("[John2, Michael3, Doe4]", patronInfo("John2", "Michael3", "Doe4")),
      arguments("[   John   ,   Michael   ,   Doe   ]", patronInfo("John", "Michael", "Doe")),
      arguments("[John,Doe]", patronInfo("John", null, "Doe")),
      arguments("[John, Doe]", patronInfo("John", null, "Doe")),
      arguments("[   John   ,  Doe  ]", patronInfo("John", null, "Doe")),
      arguments("[,Michael,Doe]", patronInfo(null, "Michael", "Doe")),
      arguments("[Doe]", patronInfo(null, null, "Doe")),
      arguments("[   Doe  ]", patronInfo(null, null, "Doe")),
      arguments("[John Michael Doe]", patronInfo(null, null, "John Michael Doe")),

      // Latin extended characters
      arguments("[Zoë, André, Müller]", patronInfo("Zoë", "André", "Müller")),
      arguments("[François, Amélie, Naïve]", patronInfo("François", "Amélie", "Naïve")),
      arguments("[Björn, Åsa, Øvredal]", patronInfo("Björn", "Åsa", "Øvredal")),
      arguments("[Søren, Niñez, Coliñon]", patronInfo("Søren", "Niñez", "Coliñon")),

      // Cyrillic characters
      arguments("[Александр, Владимир, Петров]", patronInfo("Александр", "Владимир", "Петров")),
      arguments("[Мария, Ивановна, Смирнова]", patronInfo("Мария", "Ивановна", "Смирнова")),

      // Greek characters
      arguments("[Αλέξανδρος, Νικόλαος, Παπαδόπουλος]", patronInfo("Αλέξανδρος", "Νικόλαος", "Παπαδόπουλος")),

      // Arabic characters
      arguments("[محمد, عبدالله, الحسن]", patronInfo("محمد", "عبدالله", "الحسن")),

      // Chinese characters
      arguments("[李, 小明, 王]", patronInfo("李", "小明", "王")),
      arguments("[張, 美麗, 陳]", patronInfo("張", "美麗", "陳")),

      // Japanese characters (Hiragana, Katakana, Kanji)
      arguments("[田中, ひろし, 山田]", patronInfo("田中", "ひろし", "山田")),
      arguments("[サトウ, ケンジ, タナカ]", patronInfo("サトウ", "ケンジ", "タナカ")),

      // Korean characters
      arguments("[김, 민수, 박]", patronInfo("김", "민수", "박")),

      // Hebrew characters
      arguments("[דוד, בנימין, כהן]", patronInfo("דוד", "בנימין", "כהן")),

      // Special symbols and punctuation in names
      arguments("[O'Brien, Mary-Jane, D'Angelo]", patronInfo("O'Brien", "Mary-Jane", "D'Angelo")),
      arguments("[St. John, Anne-Marie, MacD'onald]", patronInfo("St. John", "Anne-Marie", "MacD'onald")),
      arguments("[João, José-Carlos, Fernández-López]", patronInfo("João", "José-Carlos", "Fernández-López")),

      // Names with apostrophes and special punctuation
      arguments("[L'amour, Jean-Luc, D'Artagnan]", patronInfo("L'amour", "Jean-Luc", "D'Artagnan")),
      arguments("[O'Malley, Seán, Ó Briain]", patronInfo("O'Malley", "Seán", "Ó Briain")),

      // Names with periods and abbreviations
      arguments("[J.R.R., John Ronald Reuel, Tolkien]", patronInfo("J.R.R.", "John Ronald Reuel", "Tolkien")),
      arguments("[Dr. John, M.D., Smith Jr.]", patronInfo("Dr. John", "M.D.", "Smith Jr.")),

      // Mixed scripts and complex names
      arguments("[José-María, François-Xavier, García-Hernández]",
        patronInfo("José-María", "François-Xavier", "García-Hernández")),
      arguments("[Михаил, Jean-Claude, Дмитриевич]", patronInfo("Михаил", "Jean-Claude", "Дмитриевич")),

      // Emoji and modern Unicode symbols (edge cases)
      arguments("[John🌟, Mary💫, Smith✨]", patronInfo("John🌟", "Mary💫", "Smith✨")),

      // Numbers and special characters mixed with letters
      arguments("[João123, María-José456, O'Brien789]", patronInfo("João123", "María-José456", "O'Brien789")),

      // Very long names with special characters
      arguments("[Jean-Baptiste-Emmanuel, Marie-Thérèse-Antoinette, Pérez-González-Rodríguez]",
        patronInfo("Jean-Baptiste-Emmanuel", "Marie-Thérèse-Antoinette", "Pérez-González-Rodríguez")),

      // Edge case: Names with brackets in them (should still work)
      arguments("[John[Jr], Mary(Smith), Doe{Senior}]", patronInfo("John[Jr]", "Mary(Smith)", "Doe{Senior}"))
    );
  }

  public static DcbPersonal patronInfo(String firstName, String middleName, String lastName) {
    return new DcbPersonal(firstName, middleName, lastName);
  }
}
