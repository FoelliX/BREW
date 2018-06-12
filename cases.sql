-- phpMyAdmin SQL Dump
-- version 4.7.0
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Erstellungszeit: 12. Dez 2017 um 09:21
-- Server-Version: 10.1.25-MariaDB
-- PHP-Version: 7.1.7

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Datenbank: `ggwiz`
--

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `cases`
--

CREATE TABLE `cases` (
  `id` int(10) NOT NULL,
  `testcase` int(8) NOT NULL,
  `status` int(2) NOT NULL,
  `query` text NOT NULL,
  `source` text NOT NULL,
  `sink` text NOT NULL,
  `truepositive` tinyint(1) NOT NULL,
  `falsepositive` tinyint(1) NOT NULL,
  `duration` int(10) NOT NULL,
  `config` text NOT NULL,
  `expected` text NOT NULL,
  `actual` text NOT NULL,
  `entered` int(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Indizes der exportierten Tabellen
--

--
-- Indizes für die Tabelle `cases`
--
ALTER TABLE `cases`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT für exportierte Tabellen
--

--
-- AUTO_INCREMENT für Tabelle `cases`
--
ALTER TABLE `cases`
  MODIFY `id` int(10) NOT NULL AUTO_INCREMENT;COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
